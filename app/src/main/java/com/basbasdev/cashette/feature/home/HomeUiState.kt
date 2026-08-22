package com.basbasdev.cashette.feature.home

import java.math.BigDecimal

/**
 * Home is fed by five independent calls, so failure is per-section. If /accounts times
 * out you still get your hero and one inline retry on the strip, rather than a blank
 * screen apologising for a request you did not make.
 */
sealed interface Section<out T> {
    data object Loading : Section<Nothing>
    data class Data<T>(val value: T) : Section<T>
    data class Failed(val message: String) : Section<Nothing>
}

val <T> Section<T>.dataOrNull: T? get() = (this as? Section.Data<T>)?.value

/**
 * The hero's fallback ladder. "Left to spend" is the answer to "can I afford this", but
 * it needs a budget, and a new account has none — so the hero degrades to a different
 * honest question rather than to a dash.
 */
sealed interface Hero {
    /** A budget exists. The real answer. */
    data class LeftToSpend(
        val left: BigDecimal,
        val spent: BigDecimal,
        val limit: BigDecimal,
        val overBy: BigDecimal?,
    ) : Hero {
        val fraction: Float
            get() = if (limit.signum() <= 0) 0f
            else (spent.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
    }

    /** No budget, but money has moved. Answers a different question, and says so. */
    data class NetThisMonth(
        val net: BigDecimal,
        val income: BigDecimal,
        val expense: BigDecimal,
    ) : Hero

    /**
     * No budget and nothing recorded yet, but the accounts are real — which is the state
     * every established user wakes up to on the first of the month. Falls back to the one
     * figure that is always true rather than to an empty surface.
     */
    data class Available(
        val total: BigDecimal,
        val accountCount: Int,
    ) : Hero

    /** No accounts either. Home collapses to onboarding. */
    data object Untouched : Hero
}

data class CategoryBurn(
    val name: String,
    val spent: BigDecimal,
    /** Null when this category has no budget for the month; the row then shows spend only. */
    val limit: BigDecimal?,
) {
    val fraction: Float
        get() = limit?.takeIf { it.signum() > 0 }
            ?.let { (spent.toFloat() / it.toFloat()).coerceIn(0f, 1f) } ?: 0f

    val over: Boolean get() = limit != null && limit.signum() > 0 && spent > limit
}

enum class TxKind { INCOME, EXPENSE, TRANSFER }

data class TxRow(
    val id: String,
    val title: String,
    val account: String,
    val date: String,
    val amount: BigDecimal,
    val kind: TxKind,
)

data class Bill(
    val name: String,
    val amount: BigDecimal,
    val cycle: String,
    /** Negative when overdue, 0 when due today, null when the subscription has no date. */
    val daysUntil: Int?,
) {
    val urgent: Boolean get() = daysUntil != null && daysUntil <= 0
    val soon: Boolean get() = daysUntil != null && daysUntil in 1..7
}

data class AccountCard(
    val id: String,
    val name: String,
    val type: String,
    val balance: BigDecimal,
)

data class Accounts(
    val cards: List<AccountCard>,
    val pocketTotal: BigDecimal,
    val pocketCount: Int,
)

data class HomeUiState(
    val monthLabel: String = "",
    val hero: Section<Hero> = Section.Loading,
    val spending: Section<List<CategoryBurn>> = Section.Loading,
    val recent: Section<List<TxRow>> = Section.Loading,
    val bill: Section<Bill?> = Section.Loading,
    val accounts: Section<Accounts> = Section.Loading,
    val refreshing: Boolean = false,
) {
    /** Every section loaded and every one of them empty: a brand-new account. */
    val untouched: Boolean
        get() = hero.dataOrNull is Hero.Untouched &&
            recent.dataOrNull?.isEmpty() == true &&
            accounts.dataOrNull?.cards?.isEmpty() == true
}
