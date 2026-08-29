package com.basbasdev.cashette.feature.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basbasdev.cashette.auth.data.AuthRepository
import com.basbasdev.cashette.core.money.toAmount
import com.basbasdev.cashette.data.LedgerApi
import com.basbasdev.cashette.data.LedgerRefresh
import com.basbasdev.cashette.data.asApiDate
import com.basbasdev.cashette.data.model.AccountDto
import com.basbasdev.cashette.data.model.CategoryDto
import com.basbasdev.cashette.data.model.CreateSubscriptionBody
import com.basbasdev.cashette.data.model.RecordSubscriptionBody
import com.basbasdev.cashette.data.toDataMessage
import com.basbasdev.cashette.feature.home.Section
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class SubscriptionItem(
    val id: String,
    val name: String,
    val amount: BigDecimal,
    val cycle: String,
    val daysUntil: Int?,
) {
    val overdue: Boolean get() = daysUntil != null && daysUntil < 0
    val dueToday: Boolean get() = daysUntil == 0
    val soon: Boolean get() = daysUntil != null && daysUntil in 1..7

    val dueLabel: String
        get() = when {
            daysUntil == null -> "no date set"
            daysUntil < 0 -> "overdue by ${-daysUntil}d"
            daysUntil == 0 -> "due today"
            daysUntil == 1 -> "due tomorrow"
            else -> "due in ${daysUntil}d"
        }
}

data class SubscriptionsUiState(
    val items: Section<List<SubscriptionItem>> = Section.Loading,
    val accounts: List<AccountDto> = emptyList(),
    val expenseCategories: List<CategoryDto> = emptyList(),
    val monthlyTotal: BigDecimal = BigDecimal.ZERO,
    val working: Boolean = false,
    val formError: String? = null,
)

private const val WEEKS_PER_MONTH = 52.0 / 12.0

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val api: LedgerApi,
    private val auth: AuthRepository,
    private val refresh: LedgerRefresh,
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionsUiState())
    val state: StateFlow<SubscriptionsUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch { refresh.revision.drop(1).collect { load() } }
    }

    fun load() {
        val userId = auth.currentUserId ?: return
        viewModelScope.launch {
            coroutineScope {
                val subs = async { runCatching { api.subscriptions(userId) } }
                val accounts = async { runCatching { api.accounts(userId) } }
                val categories = async { runCatching { api.categories(userId) } }

                val items = subs.await().map { list ->
                    list.map { dto ->
                        val due = dto.nextBillingDate
                            ?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
                        SubscriptionItem(
                            id = dto.id,
                            name = dto.name,
                            amount = dto.amount.toAmount(),
                            cycle = dto.billingCycle,
                            daysUntil = due?.let {
                                ChronoUnit.DAYS.between(LocalDate.now(), it).toInt()
                            },
                        )
                    }.sortedBy { it.daysUntil ?: Int.MAX_VALUE }
                }

                _state.update {
                    it.copy(
                        items = items.fold(
                            onSuccess = { v -> Section.Data(v) },
                            onFailure = { e -> Section.Failed(e.toDataMessage()) },
                        ),
                        accounts = accounts.await().getOrNull().orEmpty()
                            .filter { a -> a.parentAccountId == null && a.accountType != "pocket" },
                        expenseCategories = categories.await().getOrNull().orEmpty()
                            .filter { c -> c.type == "expense" },
                        monthlyTotal = items.getOrNull().orEmpty()
                            .fold(BigDecimal.ZERO) { acc, s -> acc + s.monthlyEquivalent() },
                    )
                }
            }
        }
    }

    /** Cycles are mixed, so a total is only honest once they are on one basis. */
    private fun SubscriptionItem.monthlyEquivalent(): BigDecimal = when (cycle) {
        "yearly" -> amount.divide(BigDecimal(12), 0, java.math.RoundingMode.HALF_UP)
        "weekly" -> amount.multiply(BigDecimal.valueOf(WEEKS_PER_MONTH))
            .setScale(0, java.math.RoundingMode.HALF_UP)
        else -> amount
    }

    fun add(name: String, amount: BigDecimal, cycle: String, nextDate: LocalDate?) = write {
        api.createSubscription(
            CreateSubscriptionBody(
                userId = auth.currentUserId ?: error("Signed out."),
                name = name,
                amount = amount.toPlainString(),
                // The web assigns a colour per subscription; nothing on this screen uses
                // one, so it sends the brand olive rather than inventing a palette.
                color = "#3f4a1f",
                billingCycle = cycle,
                nextBillingDate = nextDate?.asApiDate(),
            ),
        )
    }

    fun record(id: String, accountId: String, categoryId: String) = write {
        api.recordSubscription(
            id = id,
            userId = auth.currentUserId ?: error("Signed out."),
            body = RecordSubscriptionBody(
                accountId = accountId,
                categoryId = categoryId,
                date = LocalDate.now().asApiDate(),
            ),
        )
    }

    fun remove(id: String) = write { api.deleteSubscription(id) }

    fun clearFormError() = _state.update { it.copy(formError = null) }

    private fun write(block: suspend () -> Unit) {
        _state.update { it.copy(working = true, formError = null) }
        viewModelScope.launch {
            runCatching { block() }.fold(
                onSuccess = {
                    _state.update { it.copy(working = false) }
                    refresh.invalidate()
                    load()
                },
                onFailure = { e ->
                    _state.update { it.copy(working = false, formError = e.toDataMessage()) }
                },
            )
        }
    }
}
