package com.basbasdev.cashette.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basbasdev.cashette.R
import com.basbasdev.cashette.core.money.toIdr
import com.basbasdev.cashette.core.money.toSignedIdr
import com.basbasdev.cashette.core.money.toSpokenIdr
import com.basbasdev.cashette.feature.home.Section
import com.basbasdev.cashette.feature.home.TxKind
import com.basbasdev.cashette.ui.components.Caption
import com.basbasdev.cashette.ui.components.ConfirmDialog
import com.basbasdev.cashette.ui.components.EmptyState
import com.basbasdev.cashette.ui.components.Money
import com.basbasdev.cashette.ui.components.SectionError
import com.basbasdev.cashette.ui.components.Skeleton
import com.basbasdev.cashette.ui.theme.CashetteShape
import com.basbasdev.cashette.ui.theme.CashetteText
import com.basbasdev.cashette.ui.theme.CashetteTheme

/**
 * The ledger. Answers "what did I spend on X" — the question Home's recent-activity
 * preview cannot, which is why this holds a tab.
 *
 * The range is a server query; everything else filters the loaded set in memory, so
 * typing is instant and no keystroke costs a round trip.
 */
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<LedgerEntry?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Spacer(Modifier.height(8.dp))

        SearchField(
            query = state.query,
            onQueryChange = viewModel::setQuery,
            showClear = state.filtersActive,
            onClear = viewModel::clearFilters,
        )

        Spacer(Modifier.height(8.dp))

        RangeChips(state.range, viewModel::setRange)

        Spacer(Modifier.height(8.dp))

        Filters(state, viewModel)

        Spacer(Modifier.height(8.dp))

        Column(Modifier.fillMaxSize()) {
            when (val section = state.all) {
                is Section.Loading -> Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) { repeat(6) { Skeleton(height = 44.dp) } }

                is Section.Failed -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                    SectionError(section.message, viewModel::load)
                }

                is Section.Data -> {
                    val groups = state.groups
                    if (groups.isEmpty()) {
                        EmptyState(
                            icon = R.drawable.ic_history,
                            headline = if (state.filtersActive) "Nothing matches"
                            else "Nothing recorded yet",
                            body = if (state.filtersActive) {
                                "No transactions in ${state.range.label.lowercase()} match " +
                                    "these filters. Try widening the range or clearing them."
                            } else {
                                "Every transaction lands here, searchable by note, " +
                                    "category and account. Add one with the button below."
                            },
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 96.dp,
                            ),
                        ) {
                            item { Totals(state) }

                            groups.forEach { group ->
                                item(key = "h-${group.header}") { DayHeader(group) }
                                items(
                                    count = group.entries.size,
                                    key = { group.entries[it].id },
                                ) { i ->
                                    EntryRow(group.entries[i]) { deleting = group.entries[i] }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleting?.let { entry ->
        ConfirmDialog(
            title = "Delete this transaction?",
            body = "${entry.title} · ${entry.amount.toIdr()}. Balances will be adjusted " +
                "and this cannot be undone.",
            onConfirm = { viewModel.delete(entry.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun RangeChips(selected: Range, onSelect: (Range) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Range.entries.forEach { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onSelect(range) },
                label = { Text(range.label, style = MaterialTheme.typography.labelMedium) },
                shape = CashetteShape.Pill,
            )
        }
    }
}

/**
 * The M3 search input, used docked rather than inside a [androidx.compose.material3.SearchBar].
 * A SearchBar expands over the screen to host suggestions, and here the suggestions *are*
 * the list underneath — filtering is instant and client-side, so covering the results to
 * search them would hide the only feedback the query has.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    showClear: Boolean,
    onClear: () -> Unit,
) {
    // The container is drawn here rather than inherited: standalone, InputField paints
    // only the field, and a search bar with no surface reads as a stray line of text.
    Surface(
        shape = SearchBarDefaults.inputFieldShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        SearchBarDefaults.InputField(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = {},
            expanded = false,
            onExpandedChange = {},
            placeholder = { Text("Search notes, categories, accounts") },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                if (showClear) {
                    IconButton(onClick = onClear) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Clear search and filters",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Filters(state: HistoryUiState, viewModel: HistoryViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TxKind.entries.forEach { kind ->
            FilterChip(
                selected = state.kindFilter == kind,
                onClick = { viewModel.setKind(if (state.kindFilter == kind) null else kind) },
                label = {
                    Text(
                        text = kind.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                shape = CashetteShape.Pill,
            )
        }
        state.accounts.forEach { account ->
            FilterChip(
                selected = state.accountFilter?.id == account.id,
                onClick = {
                    viewModel.setAccount(
                        if (state.accountFilter?.id == account.id) null else account,
                    )
                },
                label = { Text(account.name, style = MaterialTheme.typography.labelMedium) },
                shape = CashetteShape.Pill,
            )
        }
    }
}

@Composable
private fun Totals(state: HistoryUiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column {
            Caption("In")
            Money(
                text = state.totalIn.toIdr(),
                spoken = "${state.totalIn.toSpokenIdr()} in",
                style = CashetteText.MoneyMedium,
                color = CashetteTheme.finance.income,
            )
        }
        Column {
            Caption("Out")
            Money(
                text = state.totalOut.toIdr(),
                spoken = "${state.totalOut.toSpokenIdr()} out",
                style = CashetteText.MoneyMedium,
                color = CashetteTheme.finance.expense,
            )
        }
    }
}

@Composable
private fun DayHeader(group: DayGroup) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = group.header,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        group.subtitle?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        if (group.net.signum() != 0) {
            Money(
                text = group.net.abs().toSignedIdr(negative = group.net.signum() < 0),
                spoken = "net ${group.net.abs().toSpokenIdr()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EntryRow(entry: LedgerEntry, onLongPress: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onLongPress)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Caption(
                listOfNotNull(
                    entry.timeLabel,
                    entry.account.takeIf { it.isNotBlank() },
                    entry.category,
                ).joinToString(" · "),
            )
        }
        Spacer(Modifier.width(12.dp))
        Money(
            text = when (entry.kind) {
                TxKind.INCOME -> entry.amount.toSignedIdr(negative = false)
                TxKind.EXPENSE -> entry.amount.toSignedIdr(negative = true)
                TxKind.TRANSFER -> entry.amount.toIdr()
            },
            spoken = entry.amount.toSpokenIdr(),
            style = CashetteText.MoneySmall,
            color = when (entry.kind) {
                TxKind.INCOME -> CashetteTheme.finance.income
                TxKind.EXPENSE -> CashetteTheme.finance.expense
                TxKind.TRANSFER -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
