package com.basbasdev.cashette.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basbasdev.cashette.auth.data.AuthRepository
import com.basbasdev.cashette.data.toDataMessage
import com.basbasdev.cashette.core.money.toAmount
import com.basbasdev.cashette.data.LedgerApi
import com.basbasdev.cashette.data.model.AccountDto
import com.basbasdev.cashette.data.model.SubscriptionDto
import com.basbasdev.cashette.data.model.TransactionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: LedgerApi,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() = load(userInitiated = true)

    private fun load(userInitiated: Boolean = false) {
        val userId = auth.currentUserId ?: return
        val month = YearMonth.now()

        _state.update {
            it.copy(
                monthLabel = month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                refreshing = userInitiated,
            )
        }

        viewModelScope.launch {
            // Five calls, one round trip's worth of latency. Each lands in its own
            // section so a single failure cannot blank the screen.
            coroutineScope {
                val transactions = async { runCatching { api.transactions(userId, month.atDay(1), month.atEndOfMonth()) } }
                val accounts = async { runCatching { api.accounts(userId) } }
                val budgets = async { runCatching { api.budgets(userId, month.monthValue, month.year) } }
                val summary = async { runCatching { api.budgetSummary(userId, month.monthValue, month.year) } }
                val subs = async { runCatching { api.subscriptions(userId) } }

                val tx = transactions.await()
                val acc = accounts.await()
                val bud = budgets.await()
                val sum = summary.await()
                val sub = subs.await()

                val accountsSummary = acc.map(::accountsSummary)

                _state.update { current ->
                    current.copy(
                        hero = heroSection(sum, tx, accountsSummary.getOrNull()),
                        spending = tx.map { rows -> categoryBurn(rows, bud.getOrNull()) }.toSection(),
                        recent = tx.map(::recentRows).toSection(),
                        bill = sub.map(::nextBill).toSection(),
                        accounts = accountsSummary.toSection(),
                        refreshing = false,
                    )
                }
            }
        }
    }

    /**
     * The ladder: a budget gives the real answer; transactions alone give a different
     * honest one; a fresh month with established accounts falls back to the balance,
     * which is always true; and only a genuinely empty account reaches onboarding. No
     * rung is allowed to render an empty hero.
     */
    private fun heroSection(
        summary: Result<com.basbasdev.cashette.data.model.BudgetSummaryDto>,
        transactions: Result<List<TransactionDto>>,
        accounts: Accounts?,
    ): Section<Hero> {
        val limit = summary.getOrNull()?.totalBudget.toAmount()

        if (limit.signum() > 0) {
            val spent = summary.getOrNull()?.totalSpent.toAmount()
            val left = limit - spent
            return Section.Data(
                Hero.LeftToSpend(
                    left = left.max(BigDecimal.ZERO),
                    spent = spent,
                    limit = limit,
                    overBy = if (spent > limit) spent - limit else null,
                ),
            )
        }

        // No budget. Fall back to what transactions alone can answer.
        return transactions.fold(
            onSuccess = { rows ->
                when {
                    rows.isNotEmpty() -> {
                        val income = rows.filter { it.type == "income" }.sumAmount()
                        val expense = rows.filter { it.type == "expense" }.sumAmount()
                        Section.Data(Hero.NetThisMonth(income - expense, income, expense))
                    }

                    accounts != null && accounts.cards.isNotEmpty() ->
                        Section.Data(
                            Hero.Available(
                                total = accounts.cards.fold(BigDecimal.ZERO) { acc, c -> acc + c.balance },
                                accountCount = accounts.cards.size,
                            ),
                        )

                    else -> Section.Data(Hero.Untouched)
                }
            },
            onFailure = { Section.Failed(it.toDataMessage()) },
        )
    }

    private fun categoryBurn(
        transactions: List<TransactionDto>,
        budgets: List<com.basbasdev.cashette.data.model.BudgetDto>?,
    ): List<CategoryBurn> {
        val limitByCategory = budgets.orEmpty().associate { it.categoryId to it.monthlyLimit.toAmount() }

        return transactions
            .filter { it.type == "expense" }
            .groupBy { it.categoryId to (it.categoryName ?: "Uncategorised") }
            .map { (key, rows) ->
                CategoryBurn(
                    name = key.second,
                    spent = rows.sumAmount(),
                    limit = key.first?.let { limitByCategory[it] }?.takeIf { it.signum() > 0 },
                )
            }
            .sortedByDescending { it.spent }
            .take(5)
    }

    private fun recentRows(transactions: List<TransactionDto>): List<TxRow> =
        transactions
            .sortedByDescending { it.transactionDate }
            .take(5)
            .map { tx ->
                TxRow(
                    id = tx.id,
                    title = tx.note?.takeIf { it.isNotBlank() } ?: tx.categoryName ?: "Untitled",
                    account = tx.accountName.orEmpty(),
                    date = tx.transactionDate.toDayLabel(),
                    amount = tx.amount.toAmount(),
                    kind = when (tx.type) {
                        "income" -> TxKind.INCOME
                        "transfer" -> TxKind.TRANSFER
                        else -> TxKind.EXPENSE
                    },
                )
            }

    /** The soonest upcoming bill, which is the only one Home has room to act on. */
    private fun nextBill(subscriptions: List<SubscriptionDto>): Bill? =
        subscriptions
            .mapNotNull { sub ->
                val days = sub.nextBillingDate?.toLocalDateOrNull()
                    ?.let { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), it).toInt() }
                Bill(
                    name = sub.name,
                    amount = sub.amount.toAmount(),
                    cycle = sub.billingCycle.replaceFirstChar { it.uppercase() },
                    daysUntil = days,
                ).takeIf { days != null }
            }
            .minByOrNull { it.daysUntil ?: Int.MAX_VALUE }

    /**
     * Pockets are money already committed, so they are excluded from the strip and
     * reported separately. Mirrors isPocketAccount in the web's account-types.
     */
    private fun accountsSummary(accounts: List<AccountDto>): Accounts {
        val (pockets, spending) = accounts.partition {
            it.accountType == "pocket" || it.parentAccountId != null
        }
        return Accounts(
            cards = spending.map {
                AccountCard(it.id, it.name, it.accountType, it.balance.toAmount())
            },
            pocketTotal = pockets.fold(BigDecimal.ZERO) { acc, p -> acc + p.balance.toAmount() },
            pocketCount = pockets.size,
        )
    }

    private fun <T> Result<T>.toSection(): Section<T> = fold(
        onSuccess = { Section.Data(it) },
        onFailure = { Section.Failed(it.toDataMessage()) },
    )
}

private fun List<TransactionDto>.sumAmount(): BigDecimal =
    fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount.toAmount() }

private val DAY_LABEL = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(take(10)) }.getOrNull()

private fun String.toDayLabel(): String = toLocalDateOrNull()?.format(DAY_LABEL) ?: ""
