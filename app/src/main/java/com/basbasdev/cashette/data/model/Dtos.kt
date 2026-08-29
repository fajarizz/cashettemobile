package com.basbasdev.cashette.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wire shapes only. Amounts stay String here and become BigDecimal at the domain
// boundary; see core/money/Money.kt for why.

@Serializable
data class TransactionDto(
    val id: String,
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("account_name") val accountName: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    val type: String,
    val amount: String,
    val note: String? = null,
    @SerialName("transaction_date") val transactionDate: String,
    /**
     * When the row was written, as opposed to the day it is filed under. The column is
     * `timestamp without time zone` holding UTC, so the value carries no offset and has
     * to be read as UTC — see recordedAt in HistoryViewModel.
     */
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    @SerialName("account_type") val accountType: String,
    val balance: String,
    @SerialName("parent_account_id") val parentAccountId: String? = null,
    /** Only meaningful on a pocket; zero on a normal account. */
    @SerialName("target_amount") val targetAmount: String? = null,
)

@Serializable
data class BudgetDto(
    val id: String,
    @SerialName("category_id") val categoryId: String,
    @SerialName("monthly_limit") val monthlyLimit: String,
    val month: Int,
    val year: Int,
)

@Serializable
data class BudgetSummaryDto(
    @SerialName("total_budget") val totalBudget: String,
    @SerialName("total_spent") val totalSpent: String,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val type: String,
)

@Serializable
data class SubscriptionDto(
    val id: String,
    val name: String,
    val amount: String,
    val color: String? = null,
    @SerialName("billing_cycle") val billingCycle: String,
    @SerialName("next_billing_date") val nextBillingDate: String? = null,
)

@Serializable
data class DueSubscriptionsDto(
    @SerialName("due_today") val dueToday: List<SubscriptionDto> = emptyList(),
    @SerialName("due_soon") val dueSoon: List<SubscriptionDto> = emptyList(),
)

/** Gin sends failures as `{"error": "..."}`. Its text is better than any status code. */
@Serializable
data class ErrorDto(val error: String? = null)

@Serializable
data class DebtDto(
    val id: String,
    val name: String = "",
    /** "payable" (you owe) or "receivable" (owed to you). */
    val type: String = "payable",
    @SerialName("principal_amount") val principalAmount: String = "0",
    @SerialName("total_amount") val totalAmount: String = "0",
    @SerialName("remaining_amount") val remainingAmount: String = "0",
    @SerialName("due_date") val dueDate: String? = null,
    val status: String = "active",
)

// ── Request bodies ───────────────────────────────────────────────────────────
//
// Amounts go out as JSON numbers because the Go handlers bind them to
// decimal.Decimal, which accepts a number or a quoted string. Dates on these
// bodies bind to time.Time and therefore need RFC3339 — not the yyyy-MM-dd the
// query parameters use. See asApiTimestamp in LedgerApi.

@Serializable
data class CreateAccountBody(
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("account_type") val accountType: String,
    val balance: String,
    @SerialName("parent_account_id") val parentAccountId: String? = null,
    @SerialName("target_amount") val targetAmount: String? = null,
)

@Serializable
data class UpdateAccountBody(
    val name: String? = null,
    @SerialName("account_type") val accountType: String? = null,
    val balance: String? = null,
    @SerialName("target_amount") val targetAmount: String? = null,
)

@Serializable
data class CreateTransactionBody(
    @SerialName("user_id") val userId: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("category_id") val categoryId: String? = null,
    val type: String,
    val amount: String,
    val note: String? = null,
    @SerialName("transaction_date") val transactionDate: String,
)

@Serializable
data class CreateTransferBody(
    @SerialName("user_id") val userId: String,
    @SerialName("from_account_id") val fromAccountId: String,
    @SerialName("to_account_id") val toAccountId: String,
    val amount: String,
    val tax: String = "0",
    val note: String? = null,
    @SerialName("transaction_date") val transactionDate: String,
)

@Serializable
data class CreateBudgetBody(
    @SerialName("user_id") val userId: String,
    @SerialName("category_id") val categoryId: String,
    @SerialName("monthly_limit") val monthlyLimit: String,
    val month: Int,
    val year: Int,
)

@Serializable
data class UpdateBudgetBody(
    @SerialName("monthly_limit") val monthlyLimit: String,
)

@Serializable
data class CreateSubscriptionBody(
    @SerialName("user_id") val userId: String,
    val name: String,
    val amount: String,
    val color: String,
    @SerialName("billing_cycle") val billingCycle: String,
    @SerialName("next_billing_date") val nextBillingDate: String? = null,
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
)

@Serializable
data class RecordSubscriptionBody(
    @SerialName("account_id") val accountId: String,
    @SerialName("category_id") val categoryId: String,
    val date: String,
)

@Serializable
data class CreateDebtBody(
    @SerialName("user_id") val userId: String,
    val name: String,
    val type: String,
    @SerialName("principal_amount") val principalAmount: String,
    @SerialName("total_amount") val totalAmount: String,
    @SerialName("remaining_amount") val remainingAmount: String,
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
)

@Serializable
data class RepayDebtBody(
    val amount: String,
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("payment_date") val paymentDate: String,
)
