package com.basbasdev.cashette.feature.budget

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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.basbasdev.cashette.ui.theme.CashetteTheme

/**
 * Per-category limits for one month. The month navigator is the only period control in
 * the app — everything else is scoped to now — because a budget is inherently a month's
 * worth of intent and comparing to last month is the point.
 */
@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BudgetLine?>(null) }
    var deleting by remember { mutableStateOf<BudgetLine?>(null) }

    CashetteScreen(title = "Budget", onBack = onBack, modifier = modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            MonthBar(state, onPrev = { viewModel.showMonth(-1) }, onNext = { viewModel.showMonth(1) })

            when (val section = state.lines) {
                is Section.Loading -> Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) { repeat(4) { Skeleton(height = 76.dp) } }

                is Section.Failed -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                    SectionError(section.message, viewModel::load)
                }

                is Section.Data -> if (section.value.isEmpty()) {
                    EmptyState(
                        icon = R.drawable.ic_budget,
                        headline = "No budgets for ${state.label}",
                        body = "Set a monthly limit per category and Home will tell you " +
                            "how much of it is left before you spend.",
                        action = {
                            if (state.unbudgeted.isNotEmpty()) {
                                AddCard("Set a budget") { creating = true }
                            }
                        },
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = padding.calculateBottomPadding() + 32.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(section.value, key = { it.id }) { line ->
                            BudgetRow(
                                line = line,
                                onClick = { editing = line },
                                onDelete = { deleting = line },
                            )
                        }
                        if (state.unbudgeted.isNotEmpty()) {
                            item { AddCard("Set another budget") { creating = true } }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        var category by remember { mutableStateOf(state.unbudgeted.firstOrNull()) }
        var limit by remember { mutableStateOf("") }
        var limitError by remember { mutableStateOf<String?>(null) }

        FormSheet(
            title = "Budget for ${state.label}",
            submitLabel = "Set budget",
            submitting = state.working,
            error = state.formError,
            onDismiss = { creating = false; viewModel.clearFormError() },
            onSubmit = {
                val value = limit.toAmountOrNull()
                val chosen = category
                if (value == null) {
                    limitError = "Enter a monthly limit."
                } else if (chosen != null) {
                    viewModel.addBudget(chosen.id, value)
                    creating = false
                }
            },
        ) {
            PickerField(
                label = "Category",
                options = state.unbudgeted,
                selected = category,
                onSelect = { category = it },
                optionLabel = { it.name },
            )
            MoneyField(
                value = limit,
                onValueChange = { limit = it; limitError = null },
                label = "Monthly limit",
                error = limitError,
                imeAction = ImeAction.Done,
            )
        }
    }

    editing?.let { line ->
        var limit by remember(line.id) {
            mutableStateOf(line.limit.toBigInteger().toString())
        }
        FormSheet(
            title = line.categoryName,
            submitLabel = "Save",
            submitting = state.working,
            error = state.formError,
            onDismiss = { editing = null; viewModel.clearFormError() },
            onSubmit = {
                limit.toAmountOrNull()?.let {
                    viewModel.editBudget(line.id, it)
                    editing = null
                }
            },
        ) {
            Text(
                text = "${line.spent.toIdr()} spent this month",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MoneyField(
                value = limit,
                onValueChange = { limit = it },
                label = "Monthly limit",
                imeAction = ImeAction.Done,
            )
        }
    }

    deleting?.let { line ->
        ConfirmDialog(
            title = "Remove the ${line.categoryName} budget?",
            body = "Spending in this category still shows up everywhere else — it just " +
                "stops being measured against a limit.",
            confirmLabel = "Remove",
            onConfirm = { viewModel.removeBudget(line.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun MonthBar(state: BudgetUiState, onPrev: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Previous month",
                )
            }
            Text(
                text = state.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = onNext) {
                Icon(
                    painter = painterResource(R.drawable.ic_forward),
                    contentDescription = "Next month",
                )
            }
        }

        if (state.totalLimit.signum() > 0) {
            val over = state.totalSpent > state.totalLimit
            Spacer(Modifier.height(4.dp))
            Rail(
                fraction = (state.totalSpent.toFloat() / state.totalLimit.toFloat())
                    .coerceIn(0f, 1f),
                fill = if (over) CashetteTheme.finance.expense else MaterialTheme.colorScheme.primary,
                height = 6.dp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${state.totalSpent.toIdr()} of ${state.totalLimit.toIdr()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        } else {
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BudgetRow(line: BudgetLine, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        shape = CashetteShape.Card,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = line.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Money(
                    text = line.spent.toIdr(),
                    spoken = "${line.categoryName}, ${line.spent.toSpokenIdr()} spent",
                    style = CashetteText.MoneySmall,
                    color = if (line.over) CashetteTheme.finance.expense
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(10.dp))
            Rail(
                fraction = line.fraction,
                fill = if (line.over) CashetteTheme.finance.expense
                else MaterialTheme.colorScheme.primary,
                height = 4.dp,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (line.over) {
                        "over by ${(line.spent - line.limit).toIdr()}"
                    } else {
                        "${line.remaining.toIdr()} left of ${line.limit.toIdr()}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = "Remove budget",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = 45f },
                    )
                }
            }
        }
    }
}
