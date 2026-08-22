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
)

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    @SerialName("account_type") val accountType: String,
    val balance: String,
    @SerialName("parent_account_id") val parentAccountId: String? = null,
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
