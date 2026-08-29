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
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.basbasdev.cashette.R
import com.basbasdev.cashette.core.money.toIdr
import com.basbasdev.cashette.core.money.toSignedIdr
import com.basbasdev.cashette.core.money.toSpokenIdr
import com.basbasdev.cashette.ui.theme.CashetteTheme
import com.basbasdev.cashette.feature.home.dataOrNull
import com.basbasdev.cashette.ui.components.CashetteScreen
import com.basbasdev.cashette.ui.components.Money
import com.basbasdev.cashette.ui.components.Skeleton
import com.basbasdev.cashette.ui.theme.CashetteShape
import com.basbasdev.cashette.ui.theme.CashetteText
import java.math.BigDecimal

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
    viewModel: MoneyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            item { NetWorth(state) }
            item {
                Section(
                    title = "Accounts",
                    caption = state.accounts.dataOrNull
                        ?.let { "${it.size} account${if (it.size == 1) "" else "s"}" }
                        ?: "Bank, e-wallet and cash balances",
                    icon = R.drawable.ic_accounts,
                    onClick = onOpenAccounts,
                )
            }
            item {
                Section(
                    title = "Pockets",
                    caption = state.pockets.dataOrNull
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { pockets ->
                            val held = pockets.fold(BigDecimal.ZERO) { acc, p -> acc + p.balance }
                            "${held.toIdr()} set aside in ${pockets.size}"
                        }
                        ?: "Savings set aside, and how far along",
                    icon = R.drawable.ic_pockets,
                    onClick = onOpenPockets,
                )
            }
            item {
                Section(
                    title = "Debt",
                    caption = when {
                        state.owed.signum() > 0 && state.owedToYou.signum() > 0 ->
                            "${state.owed.toIdr()} owed · ${state.owedToYou.toIdr()} owed to you"
                        state.owed.signum() > 0 -> "${state.owed.toIdr()} outstanding"
                        state.owedToYou.signum() > 0 -> "${state.owedToYou.toIdr()} owed to you"
                        else -> "What you owe, and what's owed to you"
                    },
                    icon = R.drawable.ic_debt,
                    onClick = onOpenDebt,
                )
            }
        }
    }
}

@Composable
private fun NetWorth(state: MoneyUiState) {
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
            if (state.netWorth == null) {
                Skeleton(width = 200.dp, height = 36.dp)
            } else {
                // Owing more than you hold is a signed amount, so it wears the sign and
                // the colour. Formatting the sign inside the figure would put it between
                // "Rp" and the digits, which reads as a typo rather than a negative.
                val negative = state.netWorth.signum() < 0
                Money(
                    text = if (negative) {
                        state.netWorth.abs().toSignedIdr(negative = true)
                    } else {
                        state.netWorth.toIdr()
                    },
                    spoken = "${state.netWorth.abs().toSpokenIdr()} " +
                        if (negative) "in the red" else "net worth",
                    style = CashetteText.MoneyHero,
                    color = if (negative) CashetteTheme.finance.expense
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                // Pockets are excluded on purpose: they hold money that is already
                // counted inside its parent account, so adding them would double it.
                text = "Account balances, less what you owe",
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
