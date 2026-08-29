package com.basbasdev.cashette.feature.edging

import com.basbasdev.cashette.data.model.AccountDto
import com.basbasdev.cashette.data.model.CategoryDto
import com.basbasdev.cashette.data.model.ConsultAiResponseDto
import com.basbasdev.cashette.feature.home.Section
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class EdgingTab(val label: String) {
    COOLDOWN("Cooldown"),
    PASSED("Resisted"),
    BOUGHT("Purchases"),
}

enum class EdgingPriority(val value: Int, val label: String) {
    LOW(1, "Low Priority"),
    MEDIUM(2, "Medium Priority"),
    HIGH(3, "High Priority"),
}

val ECOMMERCE_PLATFORMS = listOf(
    "Shopee",
    "Tokopedia",
    "TikTok Shop",
    "Lazada",
    "Blibli",
    "Offline Store",
    "Other",
)

data class EdgingItemModel(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val price: BigDecimal,
    val categoryId: String?,
    val categoryName: String?,
    val priority: EdgingPriority,
    val platform: String,
    val status: String,
    val cooldownUntil: Instant,
    val boughtAt: Instant?,
    val finalPrice: BigDecimal?,
    val shippingFee: BigDecimal?,
    val buyReason: String?,
    val satisfactionScore: Int?,
    val dissatisfactionReason: String?,
    val createdAt: Instant,
) {
    val isReady: Boolean
        get() = status == "ready" || (status == "active" && !cooldownUntil.isAfter(Instant.now()))

    val remainingDuration: Duration
        get() {
            val now = Instant.now()
            return if (now.isAfter(cooldownUntil)) Duration.ZERO else Duration.between(now, cooldownUntil)
        }

    val remainingFormatted: String
        get() {
            if (isReady) return "Ready to decide"
            val d = remainingDuration
            val days = d.toDays()
            val hours = d.toHours() % 24
            return if (days > 0) "${days}d ${hours}h left" else "${hours}h left"
        }

    val createdDateFormatted: String
        get() = DateTimeFormatter.ofPattern("d MMM yyyy")
            .withZone(ZoneId.systemDefault())
            .format(createdAt)
}

data class EdgingSummaryModel(
    val activeCount: Int = 0,
    val readyCount: Int = 0,
    val boughtCount: Int = 0,
    val passedCount: Int = 0,
    val totalSaved: BigDecimal = BigDecimal.ZERO,
)

data class EdgingUiState(
    val items: Section<List<EdgingItemModel>> = Section.Loading,
    val summary: EdgingSummaryModel = EdgingSummaryModel(),
    val categories: List<CategoryDto> = emptyList(),
    val accounts: List<AccountDto> = emptyList(),
    val spendableBalance: BigDecimal = BigDecimal.ZERO,
    val selectedTab: EdgingTab = EdgingTab.COOLDOWN,
    val refreshing: Boolean = false,
    val consultAiLoading: Boolean = false,
    val consultAiResult: ConsultAiResponseDto? = null,
    val consultAiError: String? = null,
)
