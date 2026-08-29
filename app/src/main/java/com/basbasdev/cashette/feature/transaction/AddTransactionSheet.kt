package com.basbasdev.cashette.feature.transaction

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basbasdev.cashette.R
import com.basbasdev.cashette.data.model.AccountDto
import com.basbasdev.cashette.data.model.CategoryDto
import com.basbasdev.cashette.ui.components.DateField
import com.basbasdev.cashette.ui.components.FormField
import com.basbasdev.cashette.ui.components.MoneyField
import com.basbasdev.cashette.ui.components.PickerField
import com.basbasdev.cashette.ui.components.toAmountOrNull
import com.basbasdev.cashette.ui.theme.CashetteShape
import java.math.BigDecimal
import java.time.LocalDate

enum class TransactionKind(val label: String, @DrawableRes val icon: Int) {
    EXPENSE("Expense", R.drawable.ic_expense),
    INCOME("Income", R.drawable.ic_income),
    TRANSFER("Transfer", R.drawable.ic_transfer),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddTransactionSheet(
    initialKind: TransactionKind,
    onDismiss: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var kind by remember { mutableStateOf(initialKind) }

    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }

    var selectedAccount by remember { mutableStateOf<AccountDto?>(null) }
    var selectedFromAccount by remember { mutableStateOf<AccountDto?>(null) }
    var selectedToAccount by remember { mutableStateOf<AccountDto?>(null) }
    var selectedCategory by remember { mutableStateOf<CategoryDto?>(null) }

    var amountError by remember { mutableStateOf<String?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }
    var transferError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.accounts) {
        if (state.accounts.isNotEmpty()) {
            if (selectedAccount == null) selectedAccount = state.accounts.firstOrNull()
            if (selectedFromAccount == null) selectedFromAccount = state.accounts.firstOrNull()
            if (selectedToAccount == null) {
                selectedToAccount = state.accounts.getOrNull(1) ?: state.accounts.firstOrNull()
            }
        }
    }

    LaunchedEffect(kind, state.categories) {
        val filtered = when (kind) {
            TransactionKind.EXPENSE -> state.categories.filter { it.type == "expense" }
            TransactionKind.INCOME -> state.categories.filter { it.type == "income" }
            TransactionKind.TRANSFER -> emptyList()
        }
        if (selectedCategory != null && selectedCategory !in filtered) {
            selectedCategory = null
        }
    }

    fun submit() {
        val parsedAmount = amount.toAmountOrNull()
        if (parsedAmount == null || parsedAmount <= BigDecimal.ZERO) {
            amountError = "Enter an amount."
            return
        }

        when (kind) {
            TransactionKind.EXPENSE -> {
                val account = selectedAccount
                if (account == null) {
                    accountError = "Select an account."
                    return
                }
                viewModel.createExpense(
                    accountId = account.id,
                    categoryId = selectedCategory?.id,
                    amount = parsedAmount,
                    date = date,
                    note = note,
                    onSuccess = onDismiss,
                )
            }

            TransactionKind.INCOME -> {
                val account = selectedAccount
                if (account == null) {
                    accountError = "Select an account."
                    return
                }
                viewModel.createIncome(
                    accountId = account.id,
                    categoryId = selectedCategory?.id,
                    amount = parsedAmount,
                    date = date,
                    note = note,
                    onSuccess = onDismiss,
                )
            }

            TransactionKind.TRANSFER -> {
                val from = selectedFromAccount
                val to = selectedToAccount
                if (from == null || to == null) {
                    transferError = "Select both from and to accounts."
                    return
                }
                if (from.id == to.id) {
                    transferError = "From and To accounts must be different."
                    return
                }
                viewModel.createTransfer(
                    fromAccountId = from.id,
                    toAccountId = to.id,
                    amount = parsedAmount,
                    date = date,
                    note = note,
                    onSuccess = onDismiss,
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = CashetteShape.Sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text(
                text = "Record",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionKind.entries.forEach { option ->
                    FilledTonalToggleButton(
                        checked = kind == option,
                        onCheckedChange = {
                            kind = option
                            amountError = null
                            accountError = null
                            transferError = null
                            viewModel.clearFormError()
                        },
                        shapes = ToggleButtonShapes(
                            shape = CashetteShape.Pill,
                            pressedShape = CashetteShape.PillPressed,
                            checkedShape = CashetteShape.Pill,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            painter = painterResource(option.icon),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(option.label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                MoneyField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        amountError = null
                    },
                    label = "Amount",
                    error = amountError,
                )

                when (kind) {
                    TransactionKind.EXPENSE -> {
                        PickerField(
                            label = "Paid from",
                            options = state.accounts,
                            selected = selectedAccount,
                            onSelect = {
                                selectedAccount = it
                                accountError = null
                            },
                            optionLabel = { it.name },
                            error = accountError,
                            placeholder = "Select account",
                        )

                        val expenseCategories = state.categories.filter { it.type == "expense" }
                        PickerField(
                            label = "Category",
                            options = expenseCategories,
                            selected = selectedCategory,
                            onSelect = { selectedCategory = it },
                            optionLabel = { it.name },
                            placeholder = "Optional category",
                        )
                    }

                    TransactionKind.INCOME -> {
                        PickerField(
                            label = "Received into",
                            options = state.accounts,
                            selected = selectedAccount,
                            onSelect = {
                                selectedAccount = it
                                accountError = null
                            },
                            optionLabel = { it.name },
                            error = accountError,
                            placeholder = "Select account",
                        )

                        val incomeCategories = state.categories.filter { it.type == "income" }
                        PickerField(
                            label = "Category",
                            options = incomeCategories,
                            selected = selectedCategory,
                            onSelect = { selectedCategory = it },
                            optionLabel = { it.name },
                            placeholder = "Optional category",
                        )
                    }

                    TransactionKind.TRANSFER -> {
                        PickerField(
                            label = "From Account",
                            options = state.accounts,
                            selected = selectedFromAccount,
                            onSelect = {
                                selectedFromAccount = it
                                transferError = null
                            },
                            optionLabel = { it.name },
                            placeholder = "Select source account",
                        )

                        PickerField(
                            label = "To Account",
                            options = state.accounts,
                            selected = selectedToAccount,
                            onSelect = {
                                selectedToAccount = it
                                transferError = null
                            },
                            optionLabel = { it.name },
                            error = transferError,
                            placeholder = "Select destination account",
                        )
                    }
                }

                DateField(
                    date = date,
                    onDateChange = { date = it },
                    label = "Date",
                )

                FormField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Note",
                    placeholder = when (kind) {
                        TransactionKind.EXPENSE -> "e.g. Lunch at cafe"
                        TransactionKind.INCOME -> "e.g. Salary, freelance"
                        TransactionKind.TRANSFER -> "e.g. Move to savings"
                    },
                    imeAction = ImeAction.Done,
                )
            }

            state.formError?.let { errorText ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = CashetteShape.Pill,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = ::submit,
                    enabled = !state.working && !state.loadingData,
                    shape = CashetteShape.Pill,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (state.working) {
                            "Saving…"
                        } else {
                            when (kind) {
                                TransactionKind.EXPENSE -> "Record Expense"
                                TransactionKind.INCOME -> "Record Income"
                                TransactionKind.TRANSFER -> "Record Transfer"
                            }
                        },
                    )
                }
            }
        }
    }
}
