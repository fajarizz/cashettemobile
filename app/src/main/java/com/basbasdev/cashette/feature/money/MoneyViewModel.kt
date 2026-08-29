package com.basbasdev.cashette.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basbasdev.cashette.auth.data.AuthRepository
import com.basbasdev.cashette.data.LedgerApi
import com.basbasdev.cashette.data.LedgerRefresh
import com.basbasdev.cashette.data.asApiTimestamp
import com.basbasdev.cashette.data.model.AccountDto
import com.basbasdev.cashette.data.model.CreateAccountBody
import com.basbasdev.cashette.data.model.CreateDebtBody
import com.basbasdev.cashette.data.model.DebtDto
import com.basbasdev.cashette.data.model.RepayDebtBody
import com.basbasdev.cashette.data.model.UpdateAccountBody
import com.basbasdev.cashette.data.toDataMessage
import com.basbasdev.cashette.core.money.toAmount
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
import java.time.LocalDate
import javax.inject.Inject

/** An account, a pocket inside one, and a debt all read off the same two calls. */
data class Holding(
    val id: String,
    val name: String,
    val type: String,
    val balance: BigDecimal,
    val target: BigDecimal?,
    val parentId: String?,
    val parentName: String?,
) {
    val progress: Float
        get() = target?.takeIf { it.signum() > 0 }
            ?.let { (balance.toFloat() / it.toFloat()).coerceIn(0f, 1f) } ?: 0f
}

data class DebtItem(
    val id: String,
    val name: String,
    val payable: Boolean,
    val remaining: BigDecimal,
    val total: BigDecimal,
    val settled: Boolean,
) {
    val paidFraction: Float
        get() = if (total.signum() <= 0) 0f
        else ((total - remaining).toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

data class MoneyUiState(
    val accounts: Section<List<Holding>> = Section.Loading,
    val pockets: Section<List<Holding>> = Section.Loading,
    val debts: Section<List<DebtItem>> = Section.Loading,
    val netWorth: BigDecimal? = null,
    val owed: BigDecimal = BigDecimal.ZERO,
    val owedToYou: BigDecimal = BigDecimal.ZERO,
    val working: Boolean = false,
    val formError: String? = null,
)

@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val api: LedgerApi,
    private val auth: AuthRepository,
    private val refresh: LedgerRefresh,
) : ViewModel() {

    private val _state = MutableStateFlow(MoneyUiState())
    val state: StateFlow<MoneyUiState> = _state.asStateFlow()

    init {
        load()
        // Anything written elsewhere — a chat confirmation, a transaction — moves a
        // balance shown here.
        viewModelScope.launch { refresh.revision.drop(1).collect { load() } }
    }

    fun load() {
        val userId = auth.currentUserId ?: return
        viewModelScope.launch {
            coroutineScope {
                val accountsCall = async { runCatching { api.accounts(userId) } }
                val debtsCall = async { runCatching { api.debts(userId) } }
                apply(accountsCall.await(), debtsCall.await())
            }
        }
    }

    private fun apply(accounts: Result<List<AccountDto>>, debts: Result<List<DebtDto>>) {
        val holdings = accounts.map { list ->
            val byId = list.associateBy { it.id }
            list.map { dto ->
                Holding(
                    id = dto.id,
                    name = dto.name,
                    type = dto.accountType,
                    balance = dto.balance.toAmount(),
                    target = dto.targetAmount.toAmount().takeIf { it.signum() > 0 },
                    parentId = dto.parentAccountId,
                    parentName = dto.parentAccountId?.let { byId[it]?.name },
                )
            }
        }

        val spending = holdings.map { all -> all.filter { it.parentId == null && it.type != "pocket" } }
        val pockets = holdings.map { all -> all.filter { it.parentId != null || it.type == "pocket" } }

        val debtItems = debts.map { list ->
            list.map { dto ->
                DebtItem(
                    id = dto.id,
                    name = dto.name,
                    payable = dto.type != "receivable",
                    remaining = dto.remainingAmount.toAmount(),
                    total = dto.totalAmount.toAmount(),
                    settled = dto.status != "active" || dto.remainingAmount.toAmount().signum() <= 0,
                )
            }
        }

        val owe = debtItems.getOrNull().orEmpty()
            .filter { it.payable && !it.settled }
            .fold(BigDecimal.ZERO) { acc, d -> acc + d.remaining }
        val owedTo = debtItems.getOrNull().orEmpty()
            .filter { !it.payable && !it.settled }
            .fold(BigDecimal.ZERO) { acc, d -> acc + d.remaining }

        // Pockets sit inside their parent's balance, so counting both would double them.
        val holdingsTotal = spending.getOrNull().orEmpty()
            .fold(BigDecimal.ZERO) { acc, a -> acc + a.balance }

        _state.update {
            it.copy(
                accounts = spending.toSection(),
                pockets = pockets.toSection(),
                debts = debtItems.toSection(),
                netWorth = if (accounts.isSuccess) holdingsTotal + owedTo - owe else null,
                owed = owe,
                owedToYou = owedTo,
            )
        }
    }

    // ── Writes ───────────────────────────────────────────────────────────────

    fun addAccount(name: String, type: String, balance: BigDecimal) = write {
        api.createAccount(
            CreateAccountBody(
                userId = requireUser(),
                name = name,
                accountType = type,
                balance = balance.toPlainString(),
            ),
        )
    }

    fun editAccount(id: String, name: String, balance: BigDecimal) = write {
        api.updateAccount(id, UpdateAccountBody(name = name, balance = balance.toPlainString()))
    }

    fun removeAccount(id: String) = write { api.deleteAccount(id) }

    fun addPocket(name: String, parentId: String, target: BigDecimal?) = write {
        api.createAccount(
            CreateAccountBody(
                userId = requireUser(),
                name = name,
                accountType = "pocket",
                balance = "0",
                parentAccountId = parentId,
                targetAmount = target?.toPlainString() ?: "0",
            ),
        )
    }

    fun addDebt(name: String, payable: Boolean, amount: BigDecimal, accountId: String?) = write {
        api.createDebt(
            CreateDebtBody(
                userId = requireUser(),
                name = name,
                type = if (payable) "payable" else "receivable",
                principalAmount = amount.toPlainString(),
                totalAmount = amount.toPlainString(),
                remainingAmount = amount.toPlainString(),
                accountId = accountId,
            ),
        )
    }

    fun repayDebt(id: String, amount: BigDecimal, accountId: String?) = write {
        api.repayDebt(
            id = id,
            userId = requireUser(),
            body = RepayDebtBody(
                amount = amount.toPlainString(),
                accountId = accountId,
                paymentDate = LocalDate.now().asApiTimestamp(),
            ),
        )
    }

    fun removeDebt(id: String) = write { api.deleteDebt(id) }

    fun clearFormError() = _state.update { it.copy(formError = null) }

    /**
     * Every write ends the same way: reload, and tell the rest of the app. Returning the
     * error to the form rather than to a snackbar keeps it next to the field that caused
     * it, and keeps the sheet open so the input is not lost.
     */
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

    private fun requireUser(): String = auth.currentUserId ?: error("Signed out.")

    private fun <T> Result<T>.toSection(): Section<T> = fold(
        onSuccess = { Section.Data(it) },
        onFailure = { Section.Failed(it.toDataMessage()) },
    )
}
