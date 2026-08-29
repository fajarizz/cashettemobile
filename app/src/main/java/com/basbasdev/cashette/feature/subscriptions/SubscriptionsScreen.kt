package com.basbasdev.cashette.feature.subscriptions

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
import androidx.compose.material3.Button
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
import com.basbasdev.cashette.data.model.AccountDto
import com.basbasdev.cashette.data.model.CategoryDto
import com.basbasdev.cashette.feature.home.Section
import com.basbasdev.cashette.ui.components.AddCard
import com.basbasdev.cashette.ui.components.CashetteScreen
import com.basbasdev.cashette.ui.components.Caption
import com.basbasdev.cashette.ui.components.ConfirmDialog
import com.basbasdev.cashette.ui.components.EmptyState
import com.basbasdev.cashette.ui.components.FormField
import com.basbasdev.cashette.ui.components.FormSheet
import com.basbasdev.cashette.ui.components.Money
import com.basbasdev.cashette.ui.components.MoneyField
import com.basbasdev.cashette.ui.components.PickerField
import com.basbasdev.cashette.ui.components.SectionError
import com.basbasdev.cashette.ui.components.Skeleton
import com.basbasdev.cashette.ui.components.toAmountOrNull
import com.basbasdev.cashette.ui.theme.CashetteShape
import com.basbasdev.cashette.ui.theme.CashetteText
import java.time.LocalDate

private val CYCLES = listOf("monthly", "yearly", "weekly")

private fun cycleLabel(c: String) = c.replaceFirstChar { it.uppercase() }

/**
 * The bills that arrive whether or not you remember them. Recording one is a single tap
 * from this screen — the web makes you catch a toast, which is the one interaction that
 * does not survive being missed.
 */
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf<SubscriptionItem?>(null) }
    var deleting by remember { mutableStateOf<SubscriptionItem?>(null) }

    CashetteScreen(title = "Subscriptions", onBack = onBack, modifier = modifier) { padding ->
        when (val section = state.items) {
            is Section.Loading -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) { repeat(4) { Skeleton(height = 84.dp) } }

            is Section.Failed -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                SectionError(section.message, viewModel::load)
            }

            is Section.Data -> if (section.value.isEmpty()) {
                EmptyState(
                    icon = R.drawable.ic_subscriptions,
                    headline = "No recurring bills",
                    body = "Add the ones that bill on a schedule and they'll show up on " +
                        "Home the day they're due, ready to record in one tap.",
                    modifier = Modifier.padding(padding),
                    action = { AddCard("Add a subscription") { creating = true } },
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
                    item {
                        Caption(
                            "${state.monthlyTotal.toIdr()} a month across " +
                                "${section.value.size} subscription" +
                                if (section.value.size == 1) "" else "s",
                        )
                    }
                    items(section.value, key = { it.id }) { sub ->
                        SubscriptionRow(
                            sub = sub,
                            canRecord = state.accounts.isNotEmpty() &&
                                state.expenseCategories.isNotEmpty(),
                            onRecord = { recording = sub },
                            onDelete = { deleting = sub },
                        )
                    }
                    item { AddCard("Add a subscription") { creating = true } }
                }
            }
        }
    }

    if (creating) {
        var name by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var cycle by remember { mutableStateOf("monthly") }
        var nameError by remember { mutableStateOf<String?>(null) }
        var amountError by remember { mutableStateOf<String?>(null) }

        FormSheet(
            title = "New subscription",
            submitLabel = "Add",
            submitting = state.working,
            error = state.formError,
            onDismiss = { creating = false; viewModel.clearFormError() },
            onSubmit = {
                val value = amount.toAmountOrNull()
                when {
                    name.isBlank() -> nameError = "Give it a name."
                    value == null -> amountError = "Enter the amount."
                    else -> {
                        // Starts one cycle out, which is the next time it actually bills.
                        val next = when (cycle) {
                            "yearly" -> LocalDate.now().plusYears(1)
                            "weekly" -> LocalDate.now().plusWeeks(1)
                            else -> LocalDate.now().plusMonths(1)
                        }
                        viewModel.add(name.trim(), value, cycle, next)
                        creating = false
                    }
                }
            },
        ) {
            FormField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = "Name",
                placeholder = "Netflix, Spotify",
                error = nameError,
            )
            MoneyField(
                value = amount,
                onValueChange = { amount = it; amountError = null },
                label = "Amount",
                error = amountError,
            )
            PickerField(
                label = "Billing cycle",
                options = CYCLES,
                selected = cycle,
                onSelect = { cycle = it },
                optionLabel = ::cycleLabel,
            )
        }
    }

    recording?.let { sub ->
        RecordSheet(
            sub = sub,
            accounts = state.accounts,
            categories = state.expenseCategories,
            working = state.working,
            error = state.formError,
            onDismiss = { recording = null; viewModel.clearFormError() },
            onRecord = { accountId, categoryId ->
                viewModel.record(sub.id, accountId, categoryId)
                recording = null
            },
        )
    }

    deleting?.let { sub ->
        ConfirmDialog(
            title = "Delete ${sub.name}?",
            body = "Payments you already recorded stay in your history.",
            onConfirm = { viewModel.remove(sub.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun RecordSheet(
    sub: SubscriptionItem,
    accounts: List<AccountDto>,
    categories: List<CategoryDto>,
    working: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onRecord: (String, String) -> Unit,
) {
    var account by remember(sub.id) { mutableStateOf(accounts.firstOrNull()) }
    var category by remember(sub.id) { mutableStateOf(categories.firstOrNull()) }

    FormSheet(
        title = "Record ${sub.name}",
        submitLabel = "Record ${sub.amount.toIdr()}",
        submitting = working,
        error = error,
        onDismiss = onDismiss,
        onSubmit = {
            val a = account
            val c = category
            if (a != null && c != null) onRecord(a.id, c.id)
        },
    ) {
        PickerField(
            label = "Paid from",
            options = accounts,
            selected = account,
            onSelect = { account = it },
            optionLabel = { it.name },
        )
        PickerField(
            label = "Category",
            options = categories,
            selected = category,
            onSelect = { category = it },
            optionLabel = { it.name },
        )
    }
}

@Composable
private fun SubscriptionRow(
    sub: SubscriptionItem,
    canRecord: Boolean,
    onRecord: () -> Unit,
    onDelete: () -> Unit,
) {
    val urgent = sub.overdue || sub.dueToday

    Surface(
        shape = CashetteShape.Card,
        color = if (urgent) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDelete),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_subscriptions),
                    contentDescription = null,
                    tint = if (urgent) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = sub.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (urgent) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${cycleLabel(sub.cycle)} · ${sub.dueLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (urgent) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.size(12.dp))
                Money(
                    text = sub.amount.toIdr(),
                    spoken = "${sub.name}, ${sub.amount.toSpokenIdr()}",
                    style = CashetteText.MoneySmall,
                    color = if (urgent) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
            }

            // Only offered once it is actually due — a Record button on a bill three
            // weeks out invites recording a payment that has not happened.
            if (canRecord && (urgent || sub.soon)) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onRecord,
                    shape = CashetteShape.Pill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) { Text("Record payment", style = MaterialTheme.typography.titleSmall) }
            }
        }
    }
}
