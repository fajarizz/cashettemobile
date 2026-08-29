package com.basbasdev.cashette.feature.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basbasdev.cashette.R
import com.basbasdev.cashette.ui.components.EmptyState
import com.basbasdev.cashette.ui.components.SectionError
import com.basbasdev.cashette.ui.theme.CashetteShape

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.basbasdev.cashette.data.model.ModelInfoDto

@Composable
fun ChatScreen(
    onOpenAccounts: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(state.turns.size, state.thinking) {
        if (state.turns.isNotEmpty() && !state.loadingMore) {
            listState.animateScrollToItem(state.turns.lastIndex + if (state.thinking) 1 else 0)
        }
    }

    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            Box(Modifier.weight(1f)) {
                when {
                    state.historyError != null -> Box(
                        Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        SectionError(state.historyError!!, viewModel::retryHistory)
                    }

                    state.empty -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        EmptyState(
                            icon = R.drawable.ic_chat,
                            headline = "Tell Cashette what you spent",
                            body = "\"Bought coffee for 25k\" is enough — it works out the " +
                                "amount, the category and the account, then asks you to confirm.",
                        )
                    }

                    else -> Transcript(
                        state = state,
                        listState = listState,
                        topPadding = statusBar + 54.dp,
                        onLoadMore = viewModel::loadMore,
                        onConfirm = viewModel::confirm,
                        onCancel = viewModel::cancel,
                        onOpenAccounts = onOpenAccounts,
                        onOpenBudget = onOpenBudget,
                        onOpenSubscriptions = onOpenSubscriptions,
                    )
                }
            }

            ChatComposer(onSend = viewModel::send, enabled = !state.sending)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBar + 54.dp)
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.background,
                        0.7f to MaterialTheme.colorScheme.background,
                        1f to Color.Transparent,
                    ),
                ),
        )

        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            ModelSelector(
                selectedModel = state.selectedModel,
                availableModels = state.availableModels,
                onSelectModel = viewModel::selectModel,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelector(
    selectedModel: ModelInfoDto?,
    availableModels: List<ModelInfoDto>,
    onSelectModel: (ModelInfoDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }

    Surface(
        onClick = { if (availableModels.isNotEmpty()) showSheet = true },
        shape = CashetteShape.Pill,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = selectedModel?.name ?: "Select Model",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val isFree = selectedModel?.isFree ?: true
            Surface(
                shape = CashetteShape.Pill,
                color = if (isFree) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Text(
                    text = if (isFree) "Free" else "Paid",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFree) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            shape = CashetteShape.Sheet,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding(),
            ) {
                Text(
                    text = "AI Model",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Choose the model used for intent parsing and speed benchmarking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    availableModels.forEach { model ->
                        val isSelected = selectedModel?.id == model.id
                        Surface(
                            onClick = {
                                onSelectModel(model)
                                showSheet = false
                            },
                            shape = CashetteShape.Card,
                            color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = model.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Surface(
                                            shape = CashetteShape.Pill,
                                            color = if (model.isFree) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                                        ) {
                                            Text(
                                                text = if (model.isFree) "Free" else "Paid",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (model.isFree) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "${model.provider} · ${model.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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


@Composable
private fun Transcript(
    state: ChatUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    topPadding: androidx.compose.ui.unit.Dp,
    onLoadMore: () -> Unit,
    onConfirm: (String, com.basbasdev.cashette.data.model.ParseDto) -> Unit,
    onCancel: (String) -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenSubscriptions: () -> Unit,
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPadding,
            bottom = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.hasMore) {
            item(key = "more") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    OutlinedButton(onClick = onLoadMore, shape = CashetteShape.Pill) {
                        Text(
                            text = if (state.loadingMore) "Loading…" else "Earlier messages",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        items(
            count = state.turns.size,
            key = { state.turns[it].id },
        ) { index ->
            val turn = state.turns[index]
            TurnBubble(
                turn = turn,
                onConfirm = { turn.parse?.let { onConfirm(turn.id, it) } },
                onCancel = { onCancel(turn.id) },
                onOpenAccounts = onOpenAccounts,
                onOpenBudget = onOpenBudget,
                onOpenSubscriptions = onOpenSubscriptions,
            )
        }

        if (state.thinking) {
            item(key = "thinking") { Thinking() }
        }
    }
}

@Composable
private fun TurnBubble(
    turn: ChatTurn,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenSubscriptions: () -> Unit,
) {
    val mine = turn.author == Author.USER

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        // A bubble hugs its text and stops at 85% of the width. Sizing it to a fraction
        // instead makes "Huh." as wide as a paragraph, which reads as a layout bug.
        Surface(
            // The tail corner stays tight on the speaker's side, so who said what is
            // legible from shape alone rather than only from colour.
            shape = if (mine) {
                CashetteShape.Card.copy(bottomEnd = CornerSize(6.dp))
            } else {
                CashetteShape.Card.copy(bottomStart = CornerSize(6.dp))
            },
            color = when {
                mine -> MaterialTheme.colorScheme.primary
                turn.failed -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceContainer
            },
            modifier = Modifier.widthAtMostFraction(0.85f),
        ) {
            Text(
                text = turn.text,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    mine -> MaterialTheme.colorScheme.onPrimary
                    turn.failed -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        if (turn.parse != null) {
            ReplyCard(
                turn = turn,
                onConfirm = onConfirm,
                onCancel = onCancel,
                onOpenAccounts = onOpenAccounts,
                onOpenBudget = onOpenBudget,
                onOpenSubscriptions = onOpenSubscriptions,
            )
        }
    }
}

/**
 * Caps a child at a fraction of the space offered while letting it shrink to its content,
 * which `fillMaxWidth(fraction)` cannot do — that one *sets* the width. Leaving the width
 * to the content is what lets the parent's `horizontalAlignment` put the bubble on its
 * speaker's edge.
 */
private fun Modifier.widthAtMostFraction(fraction: Float) = layout { measurable, constraints ->
    val cap = if (constraints.hasBoundedWidth) {
        (constraints.maxWidth * fraction).roundToInt()
    } else {
        constraints.maxWidth
    }
    val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = cap))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/** Three dots on the assistant's side. The only motion on the screen while it thinks. */
@Composable
private fun Thinking() {
    Surface(
        shape = CashetteShape.Card.copy(bottomStart = CornerSize(6.dp)),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { i -> Dot(delayMillis = i * 150) }
        }
    }
}

@Composable
private fun Dot(delayMillis: Int) {
    val lift by rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(600, delayMillis = delayMillis),
            RepeatMode.Reverse,
        ),
        label = "lift",
    )
    Box(
        Modifier
            .graphicsLayer { translationY = -lift * 5f }
            .size(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant),
    )
}
