package com.basbasdev.cashette.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basbasdev.cashette.R
import com.basbasdev.cashette.core.money.toIdr
import com.basbasdev.cashette.ui.components.Caption
import com.basbasdev.cashette.ui.components.CashetteScreen
import com.basbasdev.cashette.ui.components.EmptyState
import com.basbasdev.cashette.ui.components.SectionError
import com.basbasdev.cashette.ui.components.SectionHeader
import com.basbasdev.cashette.ui.components.Skeleton
import com.basbasdev.cashette.ui.theme.CashetteShape
import com.basbasdev.cashette.ui.theme.CashetteText
import java.math.BigDecimal

/**
 * A descending answer, not a dashboard.
 *
 * The web renders seven equal-weight panels in a two-column grid; on a phone that is a
 * long scroll with no hierarchy. Here exactly one thing is hero-scale, and every block
 * below it is smaller, more specific, and structurally distinct — a hero surface, a
 * ranked list with bars, a ledger, one actionable row, a rack of cards. Five identical
 * rounded rectangles would be the same mistake in a taller shape.
 */
@Composable
fun HomeScreen(
    displayName: String,
    listState: LazyListState,
    onOpenSettings: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMoney: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CashetteScreen(
        title = "Cashette",
        modifier = modifier,
        actions = {
            IconButton(onClick = onOpenAnalytics) {
                Icon(
                    painter = painterResource(R.drawable.ic_analytics),
                    contentDescription = "Analytics",
                )
            }
            Avatar(displayName = displayName, onClick = onOpenSettings)
            Spacer(Modifier.width(8.dp))
        },
    ) { padding ->
        HomeContent(
            state = state,
            padding = padding,
            listState = listState,
            onRefresh = viewModel::refresh,
            onOpenBudget = onOpenBudget,
            onOpenSubscriptions = onOpenSubscriptions,
            onOpenHistory = onOpenHistory,
            onOpenMoney = onOpenMoney,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    padding: PaddingValues,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMoney: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding()),
    ) {
        if (state.untouched) {
            FirstRun(onOpenMoney = onOpenMoney)
            return@PullToRefreshBox
        }

        val bill = state.bill.dataOrNull

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                // The hero needs air under the app bar; at 8dp it read as attached to it.
                top = 20.dp,
                // Clears the bar and the FAB parked above it.
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            heroBlock(state, onOpenBudget, onRefresh)

            // Something due today outranks any analysis of the past. It only jumps the
            // queue when that is actually true; otherwise it sits in its normal place.
            if (bill != null && bill.urgent) {
                item(key = "bill-urgent") { BillRow(bill, onOpenSubscriptions) }
            }

            spendingBlock(state, onOpenBudget, onRefresh)
            recentBlock(state, onOpenHistory, onRefresh)

            if (bill != null && !bill.urgent) {
                item(key = "bill") {
                    Column {
                        SectionHeader("Next bill", "All", onOpenSubscriptions)
                        Spacer(Modifier.height(8.dp))
                        BillRow(bill, onOpenSubscriptions)
                    }
                }
            }

            // Staying silent here would be a lie of omission: an unread bill section
            // looks identical to having no bills, and one of those is due today.
            if (state.bill is Section.Failed) {
                item(key = "bill-error") {
                    Column {
                        SectionHeader("Next bill")
                        SectionError((state.bill as Section.Failed).message, onRefresh)
                    }
                }
            }

            accountsBlock(state, onOpenMoney, onRefresh)
        }
    }
}

private fun LazyListScope.heroBlock(
    state: HomeUiState,
    onOpenBudget: () -> Unit,
    onRetry: () -> Unit,
) = item(key = "hero") {
    when (val hero = state.hero) {
        is Section.Loading -> Surface(
            shape = CashetteShape.Hero,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(24.dp)) {
                Skeleton(width = 120.dp, height = 12.dp)
                Spacer(Modifier.height(12.dp))
                Skeleton(width = 200.dp, height = 36.dp)
                Spacer(Modifier.height(18.dp))
                Skeleton(height = 8.dp)
            }
        }

        is Section.Failed -> Surface(
            shape = CashetteShape.Hero,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.padding(24.dp)) { SectionError(hero.message, onRetry) }
        }

        is Section.Data -> HeroCard(hero.value, state.monthLabel, onOpenBudget)
    }
}

private fun LazyListScope.spendingBlock(
    state: HomeUiState,
    onOpenBudget: () -> Unit,
    onRetry: () -> Unit,
) {
    val rows = state.spending.dataOrNull
    if (rows != null && rows.isEmpty()) return

    item(key = "spending-header") {
        Spacer(Modifier.height(4.dp))
        SectionHeader("Where it's going", "Budget", onOpenBudget)
    }

    when (val section = state.spending) {
        is Section.Loading -> item(key = "spending-skeleton") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(3) { Skeleton(height = 14.dp) }
            }
        }

        is Section.Failed -> item(key = "spending-error") {
            SectionError(section.message, onRetry)
        }

        is Section.Data -> {
            val top = section.value.maxOfOrNull { it.spent } ?: BigDecimal.ZERO
            items(section.value, key = { "cat-${it.name}" }) { CategoryRow(it, top) }
        }
    }
}

private fun LazyListScope.recentBlock(
    state: HomeUiState,
    onOpenHistory: () -> Unit,
    onRetry: () -> Unit,
) {
    val rows = state.recent.dataOrNull
    if (rows != null && rows.isEmpty()) return

    item(key = "recent-header") {
        Spacer(Modifier.height(4.dp))
        SectionHeader("Recent", "See all", onOpenHistory)
    }

    when (val section = state.recent) {
        is Section.Loading -> item(key = "recent-skeleton") {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                repeat(3) { Skeleton(height = 14.dp) }
            }
        }

        is Section.Failed -> item(key = "recent-error") { SectionError(section.message, onRetry) }

        is Section.Data -> items(section.value, key = { it.id }) { TransactionRow(it) }
    }
}

private fun LazyListScope.accountsBlock(
    state: HomeUiState,
    onOpenMoney: () -> Unit,
    onRetry: () -> Unit,
) {
    val accounts = state.accounts.dataOrNull
    if (accounts != null && accounts.cards.isEmpty()) return

    item(key = "accounts") {
        Spacer(Modifier.height(4.dp))
        Column {
            SectionHeader("Accounts", "Manage", onOpenMoney)
            Spacer(Modifier.height(8.dp))

            when (val section = state.accounts) {
                is Section.Loading -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(2) { Skeleton(width = 168.dp, height = 116.dp) }
                }

                is Section.Failed -> SectionError(section.message, onRetry)

                is Section.Data -> {
                    // Balance cards read as objects to flick through. Uncontained rather
                    // than multi-browse: the masked variant clips the trailing card, and
                    // a half-cut money figure is worse than an unmasked edge.
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(section.value.cards, key = { it.id }) { card ->
                            AccountCardItem(card, Modifier.width(168.dp).height(116.dp))
                        }
                    }
                    if (section.value.pocketCount > 0) {
                        Spacer(Modifier.height(10.dp))
                        Caption(
                            "${section.value.pocketTotal.toIdr()} set aside in " +
                                "${section.value.pocketCount} pocket" +
                                if (section.value.pocketCount > 1) "s" else "",
                        )
                    }
                }
            }
        }
    }
}

/**
 * A brand-new account. Five empty blocks would teach nothing, so Home collapses to the
 * one thing that has to happen first.
 */
@Composable
private fun FirstRun(onOpenMoney: () -> Unit) {
    EmptyState(
        icon = R.drawable.ic_money,
        headline = "Nothing recorded yet",
        body = "Add the accounts your money actually sits in, then record as you go — " +
            "with the button below, or just tell the assistant what you spent.",
        action = {
            Surface(
                shape = CashetteShape.Pill,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onOpenMoney),
            ) {
                Text(
                    text = "Add an account",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                )
            }
        },
    )
}

@Composable
private fun Avatar(displayName: String, onClick: () -> Unit) {
    val initials = displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .semantics { role = Role.Button; contentDescription = "Your account and settings" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
