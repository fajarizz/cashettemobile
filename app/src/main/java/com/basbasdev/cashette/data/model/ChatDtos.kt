package com.basbasdev.cashette.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The chat endpoints are the one place amounts arrive as JSON *numbers* rather than
// decimal strings — the LLM path builds its own gin.H payloads. Kept as Double here and
// converted at the domain boundary so nothing downstream has to know.

@Serializable
data class BalanceResultDto(
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("account_name") val accountName: String = "",
    @SerialName("account_type") val accountType: String = "",
    val balance: Double = 0.0,
)

@Serializable
data class AccountItemDto(
    val id: String,
    val name: String,
    @SerialName("account_type") val accountType: String = "",
    val balance: Double = 0.0,
)

@Serializable
data class BudgetItemDto(
    val id: String,
    @SerialName("category_name") val categoryName: String = "",
    @SerialName("monthly_limit") val monthlyLimit: Double = 0.0,
)

@Serializable
data class SubscriptionItemDto(
    val id: String,
    val name: String = "",
    val amount: Double = 0.0,
    val color: String? = null,
    @SerialName("billing_cycle") val billingCycle: String = "",
)

/**
 * One shape for every reply. `requires_confirmation` marks the ones that changed
 * nothing yet and are waiting on the user; the `*_result` fields carry read-only
 * answers the assistant looked up.
 */
@Serializable
data class ParseDto(
    @SerialName("parsed_intent_id") val parsedIntentId: String? = null,
    val intent: String = "unknown",
    val amount: Double? = null,
    val tax: Double? = null,
    val category: String? = null,
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("account_name") val accountName: String? = null,
    @SerialName("account_balance") val accountBalance: Double? = null,
    @SerialName("to_account_id") val toAccountId: String? = null,
    @SerialName("to_account_name") val toAccountName: String? = null,
    @SerialName("to_account_balance") val toAccountBalance: Double? = null,
    val date: String? = null,
    @SerialName("reply_message") val replyMessage: String? = null,
    @SerialName("entity_name") val entityName: String? = null,
    @SerialName("entity_type") val entityType: String? = null,
    @SerialName("requires_confirmation") val requiresConfirmation: Boolean = false,
    @SerialName("balance_result") val balanceResult: BalanceResultDto? = null,
    @SerialName("accounts_result") val accountsResult: List<AccountItemDto>? = null,
    @SerialName("budgets_result") val budgetsResult: List<BudgetItemDto>? = null,
    @SerialName("subscriptions_result") val subscriptionsResult: List<SubscriptionItemDto>? = null,
) {
    /** Worth attaching a card to, rather than just a sentence. */
    val hasCard: Boolean
        get() = requiresConfirmation || balanceResult != null || accountsResult != null ||
            budgetsResult != null || subscriptionsResult != null
}

@Serializable
data class ChatHistoryMessageDto(
    val id: String,
    val role: String,
    val message: String = "",
    @SerialName("parse_data") val parseData: ParseDto? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class ChatHistoryDto(
    val messages: List<ChatHistoryMessageDto>? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class ConfirmRequestDto(
    @SerialName("parsed_intent_id") val parsedIntentId: String,
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("to_account_id") val toAccountId: String? = null,
    val amount: Double? = null,
    val tax: Double? = null,
)

@Serializable
data class ConfirmResponseDto(
    val message: String? = null,
    val error: String? = null,
)

@Serializable
data class ParseRequestDto(val message: String)
