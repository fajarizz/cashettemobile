package com.basbasdev.cashette.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EdgingDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val price: String,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    val priority: Int = 2,
    val platform: String = "Shopee",
    val status: String = "active",
    @SerialName("cooldown_until") val cooldownUntil: String,
    @SerialName("bought_at") val boughtAt: String? = null,
    @SerialName("final_price") val finalPrice: String? = null,
    @SerialName("shipping_fee") val shippingFee: String? = null,
    @SerialName("buy_reason") val buyReason: String? = null,
    @SerialName("satisfaction_score") val satisfactionScore: Int? = null,
    @SerialName("dissatisfaction_reason") val dissatisfactionReason: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class CreateEdgingBody(
    val name: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val price: Double,
    @SerialName("category_id") val categoryId: String? = null,
    val priority: Int = 2,
    val platform: String = "Shopee",
    @SerialName("cooldown_days") val cooldownDays: Int = 3,
)

@Serializable
data class ResolveEdgingBody(
    val status: String,
    @SerialName("final_price") val finalPrice: Double? = null,
    @SerialName("shipping_fee") val shippingFee: Double? = null,
    val platform: String? = null,
    @SerialName("buy_reason") val buyReason: String? = null,
)

@Serializable
data class SatisfactionBody(
    @SerialName("satisfaction_score") val satisfactionScore: Int,
    @SerialName("dissatisfaction_reason") val dissatisfactionReason: String? = null,
)

@Serializable
data class EdgingSummaryDto(
    @SerialName("active_count") val activeCount: Int = 0,
    @SerialName("ready_count") val readyCount: Int = 0,
    @SerialName("bought_count") val boughtCount: Int = 0,
    @SerialName("passed_count") val passedCount: Int = 0,
    @SerialName("total_saved") val totalSaved: String = "0",
)

@Serializable
data class ConsultAiBody(
    val name: String,
    val price: Double,
    val category: String = "",
    val priority: Int = 2,
    val platform: String = "Shopee",
    val reason: String = "",
)

@Serializable
data class ConsultAiResponseDto(
    val verdict: String = "WAIT",
    val title: String = "",
    val rationale: String = "",
    @SerialName("key_factors") val keyFactors: List<String> = emptyList(),
)
