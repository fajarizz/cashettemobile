package com.basbasdev.cashette.feature.money

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.basbasdev.cashette.R
import com.basbasdev.cashette.ui.components.CashetteScreen
import com.basbasdev.cashette.ui.theme.CashetteShape
import com.basbasdev.cashette.ui.theme.CashetteText

/**
 * The balance sheet — what the user holds and what they owe. A destination in its own
 * right rather than a menu: net position on top, then accounts, pockets and debt as
 * sections of one statement, each opening its full screen.
 *
 * Pockets and Debt have no card on Home, so this is where they live.
 */
@Composable
fun MoneyScreen(
    listState: LazyListState,
    onOpenAccounts: () -> Unit,
    onOpenPockets: () -> Unit,
    onOpenDebt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CashetteScreen(title = "Money", modifier = modifier) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 20.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { NetWorth() }
            item {
                Section(
                    title = "Accounts",
                    caption = "Bank, e-wallet and cash balances",
                    icon = R.drawable.ic_accounts,
                    onClick = onOpenAccounts,
                )
            }
            item {
                Section(
                    title = "Pockets",
                    caption = "Savings set aside, and how far along",
                    icon = R.drawable.ic_pockets,
                    onClick = onOpenPockets,
                )
            }
            item {
                Section(
                    title = "Debt",
                    caption = "What you owe, and what's owed to you",
                    icon = R.drawable.ic_debt,
                    onClick = onOpenDebt,
                )
            }
        }
    }
}

@Composable
private fun NetWorth() {
    Surface(
        shape = CashetteShape.Hero,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                text = "Net worth",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Rp —",
                style = CashetteText.MoneyHero,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Accounts and pockets, less what you owe",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(
    title: String,
    caption: String,
    icon: Int,
    onClick: () -> Unit,
) {
    Surface(
        shape = CashetteShape.Card,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
