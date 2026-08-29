package com.basbasdev.cashette.feature.money

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basbasdev.cashette.R
import com.basbasdev.cashette.core.money.toIdr
import com.basbasdev.cashette.core.money.toSpokenIdr
import com.basbasdev.cashette.feature.home.Section
import com.basbasdev.cashette.feature.home.dataOrNull
import com.basbasdev.cashette.ui.components.AddCard
import com.basbasdev.cashette.ui.components.CashetteScreen
import com.basbasdev.cashette.ui.components.ConfirmDialog
import com.basbasdev.cashette.ui.components.EmptyState
import com.basbasdev.cashette.ui.components.FormField
import com.basbasdev.cashette.ui.components.FormSheet
import com.basbasdev.cashette.ui.components.Money
import com.basbasdev.cashette.ui.components.MoneyField
import com.basbasdev.cashette.ui.components.PickerField
import com.basbasdev.cashette.ui.components.Rail
import com.basbasdev.cashette.ui.components.SectionError
import com.basbasdev.cashette.ui.components.SectionHeader
import com.basbasdev.cashette.ui.components.toAmountOrNull
import com.basbasdev.cashette.ui.theme.CashetteShape
import com.basbasdev.cashette.ui.theme.CashetteText
import com.basbasdev.cashette.ui.theme.CashetteTheme

/**
 * Two ledgers that look alike and mean opposite things, so they are split under their
 * own headings rather than mixed and colour-coded.
 */
@Composable
fun DebtScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoneyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var repaying by remember { mutableStateOf<DebtItem?>(null) }
    var deleting by remember { mutableStateOf<DebtItem?>(null) }

    val accounts = state.accounts.dataOrNull.orEmpty()

    CashetteScreen(title = "Debt & loans", onBack = onBack, modifier = modifier) { padding ->
        when (val section = state.debts) {
            is Section.Loading -> LoadingList(padding)

            is Section.Failed -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                SectionError(section.message, viewModel::load)
            }

            is Section.Data -> {
                val owed = section.value.filter { it.payable }
                val owedToYou = section.value.filter { !it.payable }

                if (section.value.isEmpty()) {
                    EmptyState(
                        icon = R.drawable.ic_debt,
                        headline = "Nothing owed either way",
                        body = "Track what you owe and what's owed to you, and record " +
                            "repayments against the account they came from.",
                        modifier = Modifier.padding(padding),
                        action = { AddCard("Add a debt") { creating = true } },
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = padding.calculateTopPadding() + 12.dp,
                            bottom = padding.calculateBottomPadding() + 32.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (owed.isNotEmpty()) {
                            item { SectionHeader("You owe") }
                            items(owed, key = { it.id }) {
                                DebtRow(it, onRepay = { repaying = it }, onDelete = { deleting = it })
                            }
                        }
                        if (owedToYou.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                SectionHeader("Owed to you")
                            }
                            items(owedToYou, key = { it.id }) {
                                DebtRow(it, onRepay = { repaying = it }, onDelete = { deleting = it })
                            }
                        }
                        item { AddCard("Add a debt") { creating = true } }
                    }
                }
            }
        }
    }

    if (creating) {
        var name by remember { mutableStateOf("") }
        var payable by remember { mutableStateOf(true) }
        var amount by remember { mutableStateOf("") }
        var account by remember { mutableStateOf<Holding?>(null) }
        var nameError by remember { mutableStateOf<String?>(null) }
        var amountError by remember { mutableStateOf<String?>(null) }

        FormSheet(
            title = "New debt",
            submitLabel = "Add debt",
            submitting = state.working,
            error = state.formError,
            onDismiss = { creating = false; viewModel.clearFormError() },
            onSubmit = {
                val value = amount.toAmountOrNull()
                when {
                    name.isBlank() -> nameError = "Give it a name."
                    value == null -> amountError = "Enter the amount."
                    else -> {
                        viewModel.addDebt(name.trim(), payable, value, account?.id)
                        creating = false
                    }
                }
            },
        ) {
            FormField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = "Name",
                placeholder = "Bank loan, money lent to Andi",
                error = nameError,
            )
            PickerField(
                label = "Direction",
                options = listOf(true, false),
                selected = payable,
                onSelect = { payable = it },
                optionLabel = { if (it) "I owe this" else "Owed to me" },
            )
            MoneyField(
                value = amount,
                onValueChange = { amount = it; amountError = null },
                label = "Amount",
                error = amountError,
            )
            PickerField(
                label = "Account (optional)",
                options = accounts,
                selected = account,
                onSelect = { account = it },
                optionLabel = { it.name },
                placeholder = "No account link",
            )
        }
    }

    repaying?.let { debt ->
        var amount by remember(debt.id) { mutableStateOf("") }
        var account by remember(debt.id) { mutableStateOf<Holding?>(null) }
        var amountError by remember(debt.id) { mutableStateOf<String?>(null) }

        FormSheet(
            title = "Record payment",
            submitLabel = "Record",
            submitting = state.working,
            error = state.formError,
            onDismiss = { repaying = null; viewModel.clearFormError() },
            onSubmit = {
                val value = amount.toAmountOrNull()
                if (value == null) {
                    amountError = "Enter the amount."
                } else {
                    viewModel.repayDebt(debt.id, value, account?.id)
                    repaying = null
                }
            },
        ) {
            Text(
                text = "${debt.name} · ${debt.remaining.toIdr()} outstanding",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MoneyField(
                value = amount,
                onValueChange = { amount = it; amountError = null },
                label = "Amount",
                error = amountError,
            )
            PickerField(
                label = "From account (optional)",
                options = accounts,
                selected = account,
                onSelect = { account = it },
                optionLabel = { it.name },
                placeholder = "No account link",
            )
        }
    }

    deleting?.let { debt ->
        ConfirmDialog(
            title = "Delete ${debt.name}?",
            body = "Repayments already recorded stay in your transaction history.",
            onConfirm = { viewModel.removeDebt(debt.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun DebtRow(debt: DebtItem, onRepay: () -> Unit, onDelete: () -> Unit) {
    Surface(
        shape = CashetteShape.Card,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDelete),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = debt.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (debt.settled) "Settled" else "of ${debt.total.toIdr()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Money(
                    text = debt.remaining.toIdr(),
                    spoken = "${debt.name}, ${debt.remaining.toSpokenIdr()} outstanding",
                    style = CashetteText.MoneySmall,
                    color = if (debt.payable) CashetteTheme.finance.expense
                    else CashetteTheme.finance.income,
                )
            }

            if (!debt.settled) {
                Spacer(Modifier.height(12.dp))
                Rail(
                    fraction = debt.paidFraction,
                    fill = MaterialTheme.colorScheme.primary,
                    height = 4.dp,
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onRepay) {
                        Text(
                            text = if (debt.payable) "Record payment" else "Record receipt",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}
