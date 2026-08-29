package com.basbasdev.cashette.feature.edging

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basbasdev.cashette.R
import com.basbasdev.cashette.core.money.toIdr
import com.basbasdev.cashette.core.money.toSpokenIdr
import com.basbasdev.cashette.feature.home.Section
import com.basbasdev.cashette.ui.components.Caption
import com.basbasdev.cashette.ui.components.CashetteScreen
import com.basbasdev.cashette.ui.components.EmptyState
import com.basbasdev.cashette.ui.components.Money
import com.basbasdev.cashette.ui.components.SectionError
import com.basbasdev.cashette.ui.components.Skeleton
import com.basbasdev.cashette.ui.theme.CashetteShape
import com.basbasdev.cashette.ui.theme.CashetteText
import com.basbasdev.cashette.ui.theme.CashetteTheme
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EdgingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var decidingItem by remember { mutableStateOf<EdgingItemModel?>(null) }
    var consultingItem by remember { mutableStateOf<EdgingItemModel?>(null) }

    CashetteScreen(title = "Desire Delay", onBack = onBack, modifier = modifier) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 12.dp,
                        bottom = padding.calculateBottomPadding() + 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "savings-hero") {
                        SavingsHeroCard(summary = state.summary)
                    }

                    item(key = "tabs-row") {
                        TabSelector(
                            selectedTab = state.selectedTab,
                            summary = state.summary,
                            onSelect = viewModel::setTab,
                        )
                    }

                    when (val itemsSection = state.items) {
                        is Section.Loading -> {
                            items(3) {
                                Surface(
                                    shape = CashetteShape.Card,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Skeleton(width = 140.dp, height = 16.dp)
                                        Spacer(Modifier.height(8.dp))
                                        Skeleton(width = 100.dp, height = 24.dp)
                                        Spacer(Modifier.height(12.dp))
                                        Skeleton(height = 36.dp)
                                    }
                                }
                            }
                        }

                        is Section.Failed -> {
                            item {
                                SectionError(itemsSection.message, onRetry = viewModel::load)
                            }
                        }

                        is Section.Data -> {
                            val filtered = when (state.selectedTab) {
                                EdgingTab.COOLDOWN -> itemsSection.value.filter { it.status == "active" || it.status == "ready" }
                                EdgingTab.PASSED -> itemsSection.value.filter { it.status == "passed" }
                                EdgingTab.BOUGHT -> itemsSection.value.filter { it.status == "bought" }
                            }

                            if (filtered.isEmpty()) {
                                item(key = "empty-tab") {
                                    when (state.selectedTab) {
                                        EdgingTab.COOLDOWN -> EmptyState(
                                            icon = R.drawable.ic_subscriptions,
                                            headline = "No active cooldowns",
                                            body = "Log items you want to buy before spending. The 3-day wait gives clarity on impulse purchases.",
                                        )

                                        EdgingTab.PASSED -> EmptyState(
                                            icon = R.drawable.ic_money,
                                            headline = "No avoided impulses yet",
                                            body = "When a 3-day cooldown finishes and you decide not to buy, your saved money accumulates here.",
                                        )

                                        EdgingTab.BOUGHT -> EmptyState(
                                            icon = R.drawable.ic_history,
                                            headline = "No purchased items",
                                            body = "Desires you verified and bought after cooling down land here for post-purchase review.",
                                        )
                                    }
                                }
                            } else {
                                items(filtered, key = { it.id }) { item ->
                                    when (state.selectedTab) {
                                        EdgingTab.COOLDOWN -> CooldownItemCard(
                                            item = item,
                                            onConsult = {
                                                consultingItem = item
                                                viewModel.consultAi(
                                                    name = item.name,
                                                    price = item.price.toDouble(),
                                                    category = item.categoryName.orEmpty(),
                                                    priority = item.priority.value,
                                                    platform = item.platform,
                                                    reason = item.buyReason.orEmpty(),
                                                )
                                            },
                                            onDecide = { decidingItem = item },
                                            onDelete = { viewModel.deleteItem(item.id) },
                                        )

                                        EdgingTab.PASSED -> PassedItemCard(
                                            item = item,
                                            onDelete = { viewModel.deleteItem(item.id) },
                                        )

                                        EdgingTab.BOUGHT -> BoughtItemCard(
                                            item = item,
                                            onUpdateSatisfaction = { score, reason ->
                                                viewModel.updateSatisfaction(item.id, score, reason)
                                            },
                                            onDelete = { viewModel.deleteItem(item.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                shape = CashetteShape.Pill,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Delay desire",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }

    if (showAddSheet) {
        AddEdgingSheet(
            categories = state.categories,
            onDismiss = { showAddSheet = false },
            onSubmit = { name, price, categoryId, priority, platform, days ->
                showAddSheet = false
                viewModel.createItem(name, price, categoryId, priority, platform, days)
            },
        )
    }

    decidingItem?.let { item ->
        EdgingDecisionSheet(
            item = item,
            spendableBalance = state.spendableBalance,
            onDismiss = { decidingItem = null },
            onPass = {
                decidingItem = null
                viewModel.resolveItem(id = item.id, status = "passed")
            },
            onBuy = { finalPrice, shippingFee, platform, buyReason ->
                decidingItem = null
                viewModel.resolveItem(
                    id = item.id,
                    status = "bought",
                    finalPrice = finalPrice,
                    shippingFee = shippingFee,
                    platform = platform,
                    buyReason = buyReason,
                )
            },
        )
    }

    consultingItem?.let { item ->
        EdgingConsultSheet(
            itemName = item.name,
            loading = state.consultAiLoading,
            result = state.consultAiResult,
            error = state.consultAiError,
            onDismiss = {
                consultingItem = null
                viewModel.clearConsultAi()
            },
        )
    }
}

@Composable
private fun SavingsHeroCard(summary: EdgingSummaryModel) {
    Surface(
        shape = CashetteShape.Hero,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "IMPULSE SAVINGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Money(
                        text = summary.totalSaved.toIdr(),
                        spoken = summary.totalSaved.toSpokenIdr(),
                        style = CashetteText.MoneyLarge,
                        color = CashetteTheme.finance.income,
                    )
                }

                Surface(
                    shape = CashetteShape.Pill,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = "${summary.passedCount} avoided",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TabSelector(
    selectedTab: EdgingTab,
    summary: EdgingSummaryModel,
    onSelect: (EdgingTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EdgingTab.entries.forEach { tab ->
            val count = when (tab) {
                EdgingTab.COOLDOWN -> summary.activeCount + summary.readyCount
                EdgingTab.PASSED -> summary.passedCount
                EdgingTab.BOUGHT -> summary.boughtCount
            }
            FilterChip(
                selected = selectedTab == tab,
                onClick = { onSelect(tab) },
                label = { Text("${tab.label} ($count)") },
                shape = CashetteShape.Pill,
            )
        }
    }
}

@Composable
private fun CooldownItemCard(
    item: EdgingItemModel,
    onConsult: () -> Unit,
    onDecide: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = CashetteShape.Card,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Money(
                        text = item.price.toIdr(),
                        spoken = item.price.toSpokenIdr(),
                        style = CashetteText.MoneyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Surface(
                    shape = CashetteShape.Pill,
                    color = if (item.isReady) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = item.remainingFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isReady) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CashetteShape.Pill,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = item.platform,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                Surface(
                    shape = CashetteShape.Pill,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = item.priority.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onConsult,
                    shape = CashetteShape.Pill,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chat),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Consult AI", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = onDecide,
                    shape = CashetteShape.Pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.isReady) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (item.isReady) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                ) {
                    Text(
                        text = if (item.isReady) "Decide Now" else "Review",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun PassedItemCard(item: EdgingItemModel, onDelete: () -> Unit) {
    Surface(
        shape = CashetteShape.Card,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = CashetteTheme.finance.income,
                    )
                    Money(
                        text = item.price.toIdr(),
                        spoken = item.price.toSpokenIdr(),
                        style = CashetteText.MoneySmall,
                        color = CashetteTheme.finance.income,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Impulse avoided · ${item.platform}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun BoughtItemCard(
    item: EdgingItemModel,
    onUpdateSatisfaction: (score: Int, reason: String?) -> Unit,
    onDelete: () -> Unit,
) {
    var rating by remember(item.satisfactionScore) { mutableIntStateOf(item.satisfactionScore ?: 0) }
    var regretReason by remember(item.dissatisfactionReason) { mutableStateOf(item.dissatisfactionReason.orEmpty()) }
    var dirty by remember { mutableStateOf(false) }

    Surface(
        shape = CashetteShape.Card,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Money(
                        text = (item.finalPrice ?: item.price).toIdr(),
                        spoken = (item.finalPrice ?: item.price).toSpokenIdr(),
                        style = CashetteText.MoneyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (item.shippingFee != null && item.shippingFee > BigDecimal.ZERO) {
                        Text(
                            text = "+ Ongkir ${item.shippingFee.toIdr()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    shape = CashetteShape.Pill,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = item.platform,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Was it worth it?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    val selected = star <= rating
                    Surface(
                        shape = CashetteShape.Pill,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .clickable {
                                rating = star
                                dirty = true
                            }
                            .padding(vertical = 2.dp),
                    ) {
                        Text(
                            text = "$star ★",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            if (rating in 1..2) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = regretReason,
                    onValueChange = {
                        regretReason = it
                        dirty = true
                    },
                    label = { Text("Why wasn't it worth it?") },
                    placeholder = { Text("e.g. Rarely used, poor quality") },
                    shape = CashetteShape.Field,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (dirty) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        dirty = false
                        onUpdateSatisfaction(rating, regretReason.ifBlank { null })
                    },
                    shape = CashetteShape.Pill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                ) {
                    Text("Save Review", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
