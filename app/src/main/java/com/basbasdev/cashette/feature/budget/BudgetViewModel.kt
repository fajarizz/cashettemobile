package com.basbasdev.cashette.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basbasdev.cashette.auth.data.AuthRepository
import com.basbasdev.cashette.core.money.toAmount
import com.basbasdev.cashette.data.LedgerApi
import com.basbasdev.cashette.data.LedgerRefresh
import com.basbasdev.cashette.data.model.CategoryDto
import com.basbasdev.cashette.data.model.CreateBudgetBody
import com.basbasdev.cashette.data.model.UpdateBudgetBody
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
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class BudgetLine(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val limit: BigDecimal,
    val spent: BigDecimal,
) {
    val fraction: Float
        get() = if (limit.signum() <= 0) 0f
        else (spent.toFloat() / limit.toFloat()).coerceIn(0f, 1f)

    val over: Boolean get() = spent > limit
    val remaining: BigDecimal get() = (limit - spent).max(BigDecimal.ZERO)
}

data class BudgetUiState(
    val period: YearMonth = YearMonth.now(),
    val lines: Section<List<BudgetLine>> = Section.Loading,
    /** Categories that have no budget this month — the only ones worth offering. */
    val unbudgeted: List<CategoryDto> = emptyList(),
    val totalLimit: BigDecimal = BigDecimal.ZERO,
    val totalSpent: BigDecimal = BigDecimal.ZERO,
    val working: Boolean = false,
    val formError: String? = null,
) {
    val label: String
        get() = "${period.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${period.year}"

    val isCurrentMonth: Boolean get() = period == YearMonth.now()
}

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val api: LedgerApi,
    private val auth: AuthRepository,
    private val refresh: LedgerRefresh,
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetUiState())
    val state: StateFlow<BudgetUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch { refresh.revision.drop(1).collect { load() } }
    }

    fun showMonth(delta: Long) {
        _state.update { it.copy(period = it.period.plusMonths(delta), lines = Section.Loading) }
        load()
    }

    fun load() {
        val userId = auth.currentUserId ?: return
        val period = _state.value.period

        viewModelScope.launch {
            coroutineScope {
                val budgets = async {
                    runCatching { api.budgets(userId, period.monthValue, period.year) }
                }
                // A budget row carries no spent figure, so burn is summed client-side
                // from the month's transactions. Both clients do this; there is no
                // server-side rollup beyond the totals in /budgets/summary.
                val transactions = async {
                    runCatching { api.transactions(userId, period.atDay(1), period.atEndOfMonth()) }
                }
                val categories = async { runCatching { api.categories(userId) } }

                val rows = budgets.await()
                val spend = transactions.await().getOrNull().orEmpty()
                    .filter { it.type == "expense" && it.categoryId != null }
                    .groupBy { it.categoryId!! }
                    .mapValues { (_, txs) ->
                        txs.fold(BigDecimal.ZERO) { acc, t -> acc + t.amount.toAmount() }
                    }
                val cats = categories.await().getOrNull().orEmpty()
                val nameById = cats.associate { it.id to it.name }

                val lines = rows.map { list ->
                    list.map { b ->
                        BudgetLine(
                            id = b.id,
                            categoryId = b.categoryId,
                            categoryName = nameById[b.categoryId] ?: "Uncategorised",
                            limit = b.monthlyLimit.toAmount(),
                            spent = spend[b.categoryId] ?: BigDecimal.ZERO,
                        )
                    }.sortedByDescending { it.fraction }
                }

                val budgeted = lines.getOrNull().orEmpty().map { it.categoryId }.toSet()

                _state.update {
                    it.copy(
                        lines = lines.fold(
                            onSuccess = { v -> Section.Data(v) },
                            onFailure = { e -> Section.Failed(e.toDataMessage()) },
                        ),
                        unbudgeted = cats.filter { c ->
                            c.type == "expense" && c.id !in budgeted
                        },
                        totalLimit = lines.getOrNull().orEmpty()
                            .fold(BigDecimal.ZERO) { acc, l -> acc + l.limit },
                        totalSpent = lines.getOrNull().orEmpty()
                            .fold(BigDecimal.ZERO) { acc, l -> acc + l.spent },
                    )
                }
            }
        }
    }

    fun addBudget(categoryId: String, limit: BigDecimal) = write {
        val period = _state.value.period
        api.createBudget(
            CreateBudgetBody(
                userId = auth.currentUserId ?: error("Signed out."),
                categoryId = categoryId,
                monthlyLimit = limit.toPlainString(),
                month = period.monthValue,
                year = period.year,
            ),
        )
    }

    fun editBudget(id: String, limit: BigDecimal) = write {
        api.updateBudget(id, UpdateBudgetBody(limit.toPlainString()))
    }

    fun removeBudget(id: String) = write { api.deleteBudget(id) }

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
