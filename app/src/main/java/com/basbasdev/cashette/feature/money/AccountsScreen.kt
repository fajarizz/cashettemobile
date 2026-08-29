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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basbasdev.cashette.R
import com.basbasdev.cashette.core.money.toIdr
import com.basbasdev.cashette.core.money.toSpokenIdr
import com.basbasdev.cashette.feature.home.Section
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
import com.basbasdev.cashette.ui.components.Skeleton
import com.basbasdev.cashette.ui.components.toAmountOrNull
import com.basbasdev.cashette.ui.theme.CashetteShape
import com.basbasdev.cashette.ui.theme.CashetteText
import java.math.BigDecimal

private val ACCOUNT_TYPES = listOf("cash", "bank", "ewallet", "credit")

private fun typeLabel(type: String) = when (type) {
    "cash" -> "Cash"
    "bank" -> "Bank"
    "ewallet" -> "E-Wallet"
    "credit" -> "Credit card"
    "pocket" -> "Pocket"
    else -> type
}

internal fun accountIconFor(type: String): Int = when (type) {
    "bank" -> R.drawable.ic_account_bank
    "ewallet" -> R.drawable.ic_account_ewallet
    "credit" -> R.drawable.ic_account_credit
    "pocket" -> R.drawable.ic_pockets
    else -> R.drawable.ic_account_cash
}

/** Where the money actually sits. Every transaction files against one of these. */
@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoneyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Holding?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Holding?>(null) }

    CashetteScreen(title = "Accounts", onBack = onBack, modifier = modifier) { padding ->
        when (val section = state.accounts) {
            is Section.Loading -> LoadingList(padding)

            is Section.Failed -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                SectionError(section.message, viewModel::load)
            }

            is Section.Data -> if (section.value.isEmpty()) {
                EmptyState(
                    icon = R.drawable.ic_accounts,
                    headline = "No accounts yet",
                    body = "Add the places your money actually sits — BCA, GoPay, cash — " +
                        "so every transaction can be filed against one.",
                    modifier = Modifier.padding(padding),
                    action = { AddCard("Add an account") { creating = true } },
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
                    items(section.value, key = { it.id }) { account ->
                        HoldingRow(
                            holding = account,
                            caption = typeLabel(account.type),
                            onClick = { editing = account },
                            onLongClick = { deleting = account },
                        )
                    }
                    item { AddCard("Add an account") { creating = true } }
                }
            }
        }
    }

    if (creating) {
        AccountSheet(
            title = "New account",
            initial = null,
            working = state.working,
            error = state.formError,
            onDismiss = { creating = false; viewModel.clearFormError() },
            onSubmit = { name, type, balance ->
                viewModel.addAccount(name, type, balance)
                creating = false
            },
        )
    }

    editing?.let { account ->
        AccountSheet(
            title = "Edit account",
            initial = account,
            working = state.working,
            error = state.formError,
            onDismiss = { editing = null; viewModel.clearFormError() },
            onSubmit = { name, _, balance ->
                viewModel.editAccount(account.id, name, balance)
                editing = null
            },
        )
    }

    deleting?.let { account ->
        ConfirmDialog(
            title = "Delete ${account.name}?",
            body = "Its transactions stay in your history but lose their account. " +
                "This cannot be undone.",
            onConfirm = { viewModel.removeAccount(account.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun AccountSheet(
    title: String,
    initial: Holding?,
    working: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, String, BigDecimal) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var type by remember { mutableStateOf(initial?.type ?: "bank") }
    var balance by remember {
        mutableStateOf(initial?.balance?.toBigInteger()?.toString().orEmpty())
    }
    var nameError by remember { mutableStateOf<String?>(null) }

    FormSheet(
        title = title,
        submitLabel = if (initial == null) "Add account" else "Save",
        submitting = working,
        error = error,
        onDismiss = onDismiss,
        onSubmit = {
            if (name.isBlank()) {
                nameError = "Give the account a name."
            } else {
                onSubmit(name.trim(), type, balance.toAmountOrNull() ?: BigDecimal.ZERO)
            }
        },
    ) {
        FormField(
            value = name,
            onValueChange = { name = it; nameError = null },
            label = "Name",
            placeholder = "BCA, GoPay, Cash",
            error = nameError,
        )
        if (initial == null) {
            PickerField(
                label = "Type",
                options = ACCOUNT_TYPES,
                selected = type,
                onSelect = { type = it },
                optionLabel = ::typeLabel,
            )
        }
        MoneyField(
            value = balance,
            onValueChange = { balance = it },
            label = if (initial == null) "Starting balance" else "Balance",
            imeAction = ImeAction.Done,
        )
    }
}

// ── Shared row, used by Accounts and Pockets ─────────────────────────────────

@Composable
internal fun HoldingRow(
    holding: Holding,
    caption: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        shape = CashetteShape.Card,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(accountIconFor(holding.type)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = holding.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.size(12.dp))
                Money(
                    text = holding.balance.toIdr(),
                    spoken = "${holding.name}, ${holding.balance.toSpokenIdr()}",
                    style = CashetteText.MoneySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            holding.target?.let { target ->
                Spacer(Modifier.height(12.dp))
                Rail(
                    fraction = holding.progress,
                    fill = MaterialTheme.colorScheme.primary,
                    height = 4.dp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "of ${target.toIdr()} target",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun LoadingList(padding: PaddingValues) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(4) { Skeleton(height = 72.dp) }
    }
}
