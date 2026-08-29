package com.basbasdev.cashette.feature.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basbasdev.cashette.auth.data.AuthRepository
import com.basbasdev.cashette.data.LedgerApi
import com.basbasdev.cashette.data.LedgerRefresh
import com.basbasdev.cashette.data.asApiTimestamp
import com.basbasdev.cashette.data.model.AccountDto
import com.basbasdev.cashette.data.model.CategoryDto
import com.basbasdev.cashette.data.model.CreateTransactionBody
import com.basbasdev.cashette.data.model.CreateTransferBody
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
import javax.inject.Inject

data class AddTransactionUiState(
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val loadingData: Boolean = true,
    val working: Boolean = false,
    val formError: String? = null,
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val api: LedgerApi,
    private val auth: AuthRepository,
    private val refresh: LedgerRefresh,
) : ViewModel() {

    private val _state = MutableStateFlow(AddTransactionUiState())
    val state: StateFlow<AddTransactionUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val userId = auth.currentUserId ?: return
        viewModelScope.launch {
            _state.update { it.copy(loadingData = true, formError = null) }
            runCatching {
                coroutineScope {
                    val accountsDeferred = async { api.accounts(userId) }
                    val categoriesDeferred = async { api.categories(userId) }
                    accountsDeferred.await() to categoriesDeferred.await()
                }
            }.fold(
                onSuccess = { (accounts, categories) ->
                    _state.update {
                        it.copy(
                            accounts = accounts,
                            categories = categories,
                            loadingData = false,
                        )
                    }
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            loadingData = false,
                            formError = err.message ?: "Failed to load accounts and categories",
                        )
                    }
                },
            )
        }
    }

    fun clearFormError() = _state.update { it.copy(formError = null) }

    fun createExpense(
        accountId: String,
        categoryId: String?,
        amount: BigDecimal,
        date: LocalDate,
        note: String?,
        onSuccess: () -> Unit,
    ) {
        val userId = auth.currentUserId ?: return
        viewModelScope.launch {
            _state.update { it.copy(working = true, formError = null) }
            runCatching {
                api.createTransaction(
                    CreateTransactionBody(
                        userId = userId,
                        accountId = accountId,
                        categoryId = categoryId,
                        type = "expense",
                        amount = amount.toPlainString(),
                        note = note?.takeIf { it.isNotBlank() },
                        transactionDate = date.asApiTimestamp(),
                    ),
                )
            }.fold(
                onSuccess = {
                    refresh.invalidate()
                    _state.update { it.copy(working = false) }
                    onSuccess()
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            working = false,
                            formError = err.message ?: "Failed to record expense",
                        )
                    }
                },
            )
        }
    }

    fun createIncome(
        accountId: String,
        categoryId: String?,
        amount: BigDecimal,
        date: LocalDate,
        note: String?,
        onSuccess: () -> Unit,
    ) {
        val userId = auth.currentUserId ?: return
        viewModelScope.launch {
            _state.update { it.copy(working = true, formError = null) }
            runCatching {
                api.createTransaction(
                    CreateTransactionBody(
                        userId = userId,
                        accountId = accountId,
                        categoryId = categoryId,
                        type = "income",
                        amount = amount.toPlainString(),
                        note = note?.takeIf { it.isNotBlank() },
                        transactionDate = date.asApiTimestamp(),
                    ),
                )
            }.fold(
                onSuccess = {
                    refresh.invalidate()
                    _state.update { it.copy(working = false) }
                    onSuccess()
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            working = false,
                            formError = err.message ?: "Failed to record income",
                        )
                    }
                },
            )
        }
    }

    fun createTransfer(
        fromAccountId: String,
        toAccountId: String,
        amount: BigDecimal,
        date: LocalDate,
        note: String?,
        onSuccess: () -> Unit,
    ) {
        val userId = auth.currentUserId ?: return
        viewModelScope.launch {
            _state.update { it.copy(working = true, formError = null) }
            runCatching {
                api.createTransfer(
                    CreateTransferBody(
                        userId = userId,
                        fromAccountId = fromAccountId,
                        toAccountId = toAccountId,
                        amount = amount.toPlainString(),
                        note = note?.takeIf { it.isNotBlank() },
                        transactionDate = date.asApiTimestamp(),
                    ),
                )
            }.fold(
                onSuccess = {
                    refresh.invalidate()
                    _state.update { it.copy(working = false) }
                    onSuccess()
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            working = false,
                            formError = err.message ?: "Failed to record transfer",
                        )
                    }
                },
            )
        }
    }
}
