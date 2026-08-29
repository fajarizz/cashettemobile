package com.basbasdev.cashette.feature.edging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basbasdev.cashette.auth.data.AuthRepository
import com.basbasdev.cashette.core.money.toAmount
import com.basbasdev.cashette.data.LedgerApi
import com.basbasdev.cashette.data.model.ConsultAiBody
import com.basbasdev.cashette.data.model.CreateEdgingBody
import com.basbasdev.cashette.data.model.EdgingDto
import com.basbasdev.cashette.data.model.ResolveEdgingBody
import com.basbasdev.cashette.data.model.SatisfactionBody
import com.basbasdev.cashette.feature.home.Section
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class EdgingViewModel @Inject constructor(
    private val api: LedgerApi,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EdgingUiState())
    val state: StateFlow<EdgingUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun setTab(tab: EdgingTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun load() {
        val userId = auth.currentUserId ?: return
        viewModelScope.launch {
            _state.update { it.copy(items = Section.Loading) }
            try {
                val dtos = runCatching { api.edgingItems(userId) }.getOrDefault(emptyList())
                val summaryDto = runCatching { api.edgingSummary(userId) }.getOrNull()
                val categories = runCatching { api.categories(userId) }.getOrDefault(emptyList())
                val accounts = runCatching { api.accounts(userId) }.getOrDefault(emptyList())

                val spendable = accounts
                    .filter { it.parentAccountId == null }
                    .fold(BigDecimal.ZERO) { acc, a -> acc.add(a.balance.toAmount()) }

                val models = dtos.map { it.toModel() }
                val summaryModel = EdgingSummaryModel(
                    activeCount = summaryDto?.activeCount ?: models.count { it.status == "active" && !it.isReady },
                    readyCount = summaryDto?.readyCount ?: models.count { it.isReady && it.status != "bought" && it.status != "passed" },
                    boughtCount = summaryDto?.boughtCount ?: models.count { it.status == "bought" },
                    passedCount = summaryDto?.passedCount ?: models.count { it.status == "passed" },
                    totalSaved = summaryDto?.totalSaved?.toAmount()
                        ?: models.filter { it.status == "passed" }.fold(BigDecimal.ZERO) { acc, m -> acc.add(m.price) },
                )

                _state.update {
                    it.copy(
                        items = Section.Data(models),
                        summary = summaryModel,
                        categories = categories,
                        accounts = accounts,
                        spendableBalance = spendable,
                    )
                }
            } catch (err: Exception) {
                _state.update { it.copy(items = Section.Failed(err.message ?: "Failed to load desires")) }
            }
        }
    }

    fun refresh() {
        val userId = auth.currentUserId ?: return
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            try {
                val dtos = runCatching { api.edgingItems(userId) }.getOrDefault(emptyList())
                val summaryDto = runCatching { api.edgingSummary(userId) }.getOrNull()
                val categories = runCatching { api.categories(userId) }.getOrDefault(emptyList())
                val accounts = runCatching { api.accounts(userId) }.getOrDefault(emptyList())

                val spendable = accounts
                    .filter { it.parentAccountId == null }
                    .fold(BigDecimal.ZERO) { acc, a -> acc.add(a.balance.toAmount()) }

                val models = dtos.map { it.toModel() }
                val summaryModel = EdgingSummaryModel(
                    activeCount = summaryDto?.activeCount ?: models.count { it.status == "active" && !it.isReady },
                    readyCount = summaryDto?.readyCount ?: models.count { it.isReady && it.status != "bought" && it.status != "passed" },
                    boughtCount = summaryDto?.boughtCount ?: models.count { it.status == "bought" },
                    passedCount = summaryDto?.passedCount ?: models.count { it.status == "passed" },
                    totalSaved = summaryDto?.totalSaved?.toAmount()
                        ?: models.filter { it.status == "passed" }.fold(BigDecimal.ZERO) { acc, m -> acc.add(m.price) },
                )

                _state.update {
                    it.copy(
                        items = Section.Data(models),
                        summary = summaryModel,
                        categories = categories,
                        accounts = accounts,
                        spendableBalance = spendable,
                        refreshing = false,
                    )
                }
            } catch (err: Exception) {
                _state.update { it.copy(refreshing = false) }
            }
        }
    }

    fun createItem(
        name: String,
        price: Double,
        categoryId: String?,
        priority: Int,
        platform: String,
        cooldownDays: Int = 3,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching {
                api.createEdgingItem(
                    CreateEdgingBody(
                        name = name,
                        price = price,
                        categoryId = categoryId,
                        priority = priority,
                        platform = platform,
                        cooldownDays = cooldownDays,
                    )
                )
            }.onSuccess {
                load()
                onSuccess()
            }
        }
    }

    fun resolveItem(
        id: String,
        status: String,
        finalPrice: Double? = null,
        shippingFee: Double? = null,
        platform: String? = null,
        buyReason: String? = null,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching {
                api.resolveEdgingItem(
                    id = id,
                    body = ResolveEdgingBody(
                        status = status,
                        finalPrice = finalPrice,
                        shippingFee = shippingFee,
                        platform = platform,
                        buyReason = buyReason,
                    )
                )
            }.onSuccess {
                load()
                onSuccess()
            }
        }
    }

    fun updateSatisfaction(
        id: String,
        score: Int,
        dissatisfactionReason: String?,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching {
                api.updateEdgingSatisfaction(
                    id = id,
                    body = SatisfactionBody(
                        satisfactionScore = score,
                        dissatisfactionReason = dissatisfactionReason,
                    )
                )
            }.onSuccess {
                load()
                onSuccess()
            }
        }
    }

    fun deleteItem(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                api.deleteEdgingItem(id)
            }.onSuccess {
                load()
                onSuccess()
            }
        }
    }

    fun consultAi(
        name: String,
        price: Double,
        category: String,
        priority: Int,
        platform: String,
        reason: String,
    ) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    consultAiLoading = true,
                    consultAiResult = null,
                    consultAiError = null,
                )
            }
            try {
                val res = api.consultAiEdging(
                    ConsultAiBody(
                        name = name,
                        price = price,
                        category = category,
                        priority = priority,
                        platform = platform,
                        reason = reason,
                    )
                )
                _state.update {
                    it.copy(
                        consultAiLoading = false,
                        consultAiResult = res,
                    )
                }
            } catch (err: Exception) {
                _state.update {
                    it.copy(
                        consultAiLoading = false,
                        consultAiError = err.message ?: "Failed to consult AI",
                    )
                }
            }
        }
    }

    fun clearConsultAi() {
        _state.update {
            it.copy(
                consultAiLoading = false,
                consultAiResult = null,
                consultAiError = null,
            )
        }
    }

    private fun EdgingDto.toModel(): EdgingItemModel {
        val prio = when (priority) {
            1 -> EdgingPriority.LOW
            3 -> EdgingPriority.HIGH
            else -> EdgingPriority.MEDIUM
        }

        val cooldown = runCatching { Instant.parse(cooldownUntil) }.getOrDefault(Instant.now())
        val created = runCatching { Instant.parse(createdAt) }.getOrDefault(Instant.now())
        val bought = boughtAt?.let { runCatching { Instant.parse(it) }.getOrNull() }

        return EdgingItemModel(
            id = id,
            name = name,
            imageUrl = imageUrl,
            price = price.toAmount(),
            categoryId = categoryId,
            categoryName = categoryName,
            priority = prio,
            platform = platform,
            status = status,
            cooldownUntil = cooldown,
            boughtAt = bought,
            finalPrice = finalPrice?.toAmount(),
            shippingFee = shippingFee?.toAmount(),
            buyReason = buyReason,
            satisfactionScore = satisfactionScore,
            dissatisfactionReason = dissatisfactionReason,
            createdAt = created,
        )
    }
}
