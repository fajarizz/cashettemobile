package com.basbasdev.cashette.feature.money

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basbasdev.cashette.R
import com.basbasdev.cashette.core.money.toIdr
import com.basbasdev.cashette.core.money.toSignedIdr
import com.basbasdev.cashette.core.money.toSpokenIdr
import com.basbasdev.cashette.feature.home.Section
import com.basbasdev.cashette.ui.components.AddCard
import com.basbasdev.cashette.ui.components.CashetteScreen
import com.basbasdev.cashette.ui.components.Money
import com.basbasdev.cashette.ui.components.Skeleton
import com.basbasdev.cashette.ui.theme.CashetteMotion
import com.basbasdev.cashette.ui.theme.CashetteShape
import com.basbasdev.cashette.ui.theme.CashetteText
import com.basbasdev.cashette.ui.theme.CashetteTheme
import java.math.BigDecimal

@Composable
fun MoneyScreen(
    listState: LazyListState,
    onOpenAccounts: () -> Unit,
    onOpenAccount: (String) -> Unit = { onOpenAccounts() },
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
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { NetWorth(state) }

            item {
                AccountsStackedSection(
                    accountsSection = state.accounts,
                    onOpenAccounts = onOpenAccounts,
                    onOpenAccount = onOpenAccount,
                )
            }

            item {
                PocketsOverviewSection(
                    pocketsSection = state.pockets,
                    onOpenPockets = onOpenPockets,
                )
            }

            item {
                DebtOverviewSection(
                    state = state,
                    onOpenDebt = onOpenDebt,
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
                text = "Account balances, less what you owe",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccountsStackedSection(
    accountsSection: Section<List<Holding>>,
    onOpenAccounts: () -> Unit,
    onOpenAccount: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                when (accountsSection) {
                    is Section.Data -> {
                        val total = accountsSection.value.fold(BigDecimal.ZERO) { acc, a -> acc + a.balance }
                        Text(
                            text = "${accountsSection.value.size} accounts · ${total.toIdr()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> Unit
                }
            }
            if (accountsSection is Section.Data && accountsSection.value.size > 1) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        text = if (expanded) "Stack" else "Expand",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        when (accountsSection) {
            is Section.Loading -> {
                Skeleton(height = 180.dp)
            }
            is Section.Failed -> {
                Surface(
                    shape = CashetteShape.Card,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAccounts),
                ) {
                    Row(
                        Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_accounts),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "Could not load accounts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            is Section.Data -> {
                val list = accountsSection.value
                if (list.isEmpty()) {
                    AddCard("Add an account") { onOpenAccounts() }
                } else if (expanded || list.size == 1) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                    ) {
                        list.forEachIndexed { index, holding ->
                            CreditCardView(
                                holding = holding,
                                cardIndex = index,
                                onClick = { onOpenAccount(holding.id) },
                            )
                        }
                    }
                } else {
                    StackedCardDeck(
                        accounts = list,
                        onCardClick = onOpenAccount,
                    )
                }
            }
        }
    }
}

@Composable
private fun StackedCardDeck(
    accounts: List<Holding>,
    onCardClick: (String) -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(CashetteMotion.fastSpatial()),
    ) {
        accounts.forEachIndexed { index, holding ->
            val isSelected = index == selectedIndex
            CreditCardView(
                holding = holding,
                cardIndex = index,
                isExpanded = isSelected,
                onClick = {
                    if (isSelected) {
                        onCardClick(holding.id)
                    } else {
                        selectedIndex = index
                    }
                },
            )
        }
    }
}

@Composable
private fun CreditCardView(
    holding: Holding,
    cardIndex: Int,
    isExpanded: Boolean = true,
    onClick: () -> Unit,
) {
    val cardColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface,
        MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface,
        MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurface,
        MaterialTheme.colorScheme.surfaceContainerLow to MaterialTheme.colorScheme.onSurface,
    )
    val (bgColor, textColor) = cardColors[cardIndex % cardColors.size]

    val badgeBg = when (holding.type) {
        "bank" -> MaterialTheme.colorScheme.secondaryContainer
        "ewallet" -> MaterialTheme.colorScheme.tertiaryContainer
        "credit" -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val badgeTextColor = when (holding.type) {
        "bank" -> MaterialTheme.colorScheme.onSecondaryContainer
        "ewallet" -> MaterialTheme.colorScheme.onTertiaryContainer
        "credit" -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = CashetteShape.Hero,
        color = bgColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(CashetteMotion.fastSpatial())
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = CashetteShape.Pill,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(accountIconFor(holding.type)),
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Text(
                        text = holding.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!isExpanded) {
                        Money(
                            text = holding.balance.toIdr(),
                            spoken = "${holding.name} balance ${holding.balance.toSpokenIdr()}",
                            style = CashetteText.MoneyMedium,
                            color = textColor,
                        )
                    }

                    Surface(
                        shape = CashetteShape.Pill,
                        color = badgeBg,
                    ) {
                        Text(
                            text = holding.type.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(CashetteMotion.fastSpatial()) + expandVertically(CashetteMotion.fastSpatial()),
                exit = fadeOut(CashetteMotion.fastSpatial()) + shrinkVertically(CashetteMotion.fastSpatial()),
            ) {
                Column {
                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .width(32.dp)
                                .height(24.dp),
                        ) {}

                        Text(
                            text = "•••• •••• •••• ${holding.name.takeLast(4).padStart(4, '0')}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            letterSpacing = 1.5.sp,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Column {
                        Text(
                            text = "Available Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(2.dp))
                        Money(
                            text = holding.balance.toIdr(),
                            spoken = "${holding.name} balance ${holding.balance.toSpokenIdr()}",
                            style = CashetteText.MoneyLarge,
                            color = textColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PocketsOverviewSection(
    pocketsSection: Section<List<Holding>>,
    onOpenPockets: () -> Unit,
) {
    Surface(
        shape = CashetteShape.Card,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPockets),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pockets),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = "Pockets",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Icon(
                    painter = painterResource(R.drawable.ic_forward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            when (pocketsSection) {
                is Section.Loading -> Skeleton(height = 20.dp, width = 160.dp)
                is Section.Failed -> Text(
                    text = "Savings set aside",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is Section.Data -> {
                    val pockets = pocketsSection.value
                    if (pockets.isEmpty()) {
                        Text(
                            text = "No savings pockets yet — tap to set one aside",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val held = pockets.fold(BigDecimal.ZERO) { acc, p -> acc + p.balance }
                        Text(
                            text = "${held.toIdr()} set aside across ${pockets.size} pocket${if (pockets.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            pockets.take(3).forEach { pocket ->
                                Surface(
                                    shape = CashetteShape.Pill,
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text(
                                            text = pocket.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                        )
                                        Text(
                                            text = pocket.balance.toIdr(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtOverviewSection(
    state: MoneyUiState,
    onOpenDebt: () -> Unit,
) {
    Surface(
        shape = CashetteShape.Card,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDebt),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_debt),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = "Debt & Receivables",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Icon(
                    painter = painterResource(R.drawable.ic_forward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CashetteShape.Card,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = "You owe",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Money(
                            text = state.owed.toIdr(),
                            spoken = "You owe ${state.owed.toSpokenIdr()}",
                            style = CashetteText.MoneyMedium,
                            color = if (state.owed.signum() > 0) CashetteTheme.finance.expense else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Surface(
                    shape = CashetteShape.Card,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = "Owed to you",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Money(
                            text = state.owedToYou.toIdr(),
                            spoken = "Owed to you ${state.owedToYou.toSpokenIdr()}",
                            style = CashetteText.MoneyMedium,
                            color = if (state.owedToYou.signum() > 0) CashetteTheme.finance.income else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
