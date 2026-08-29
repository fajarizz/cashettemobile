package com.basbasdev.cashette.feature.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basbasdev.cashette.R
import com.basbasdev.cashette.core.money.toIdr
import com.basbasdev.cashette.core.money.toSignedIdr
import com.basbasdev.cashette.core.money.toSpokenIdr
import com.basbasdev.cashette.ui.components.Caption
import com.basbasdev.cashette.ui.components.Money
import com.basbasdev.cashette.ui.components.Rail
import com.basbasdev.cashette.ui.theme.CashetteMotion
import com.basbasdev.cashette.ui.theme.CashetteShape
import com.basbasdev.cashette.ui.theme.CashetteText
import com.basbasdev.cashette.ui.theme.CashetteTheme
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── The hero ─────────────────────────────────────────────────────────────────

/**
 * The one thing at hero scale, and the only reason to open Home standing at a till.
 * Three faces, chosen by what the account can actually answer — see [Hero].
 */
@Composable
fun HeroCard(hero: Hero, monthLabel: String, onOpenBudget: () -> Unit) {
    Surface(
        shape = CashetteShape.Hero,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open budget", onClick = onOpenBudget),
    ) {
        Column(Modifier.padding(24.dp)) {
            when (hero) {
                is Hero.LeftToSpend -> LeftToSpendFace(hero, monthLabel)
                is Hero.NetThisMonth -> NetFace(hero, monthLabel, onOpenBudget)
                is Hero.Available -> AvailableFace(hero, monthLabel, onOpenBudget)
                Hero.Untouched -> Unit
            }
        }
    }
}

@Composable
private fun LeftToSpendFace(hero: Hero.LeftToSpend, monthLabel: String) {
    val over = hero.overBy != null
    val amount = hero.overBy ?: hero.left
    val tint = if (over) CashetteTheme.finance.expense else MaterialTheme.colorScheme.onSurface

    Caption(if (over) "Over budget · $monthLabel" else "Left to spend · $monthLabel")
    Spacer(Modifier.height(6.dp))
    Money(
        text = if (over) "${amount.toIdr()} over" else amount.toIdr(),
        spoken = if (over) "${amount.toSpokenIdr()} over budget" else "${amount.toSpokenIdr()} left to spend",
        style = CashetteText.MoneyHero,
        color = tint,
    )
    Spacer(Modifier.height(16.dp))
    Rail(
        fraction = hero.fraction,
        fill = if (over) CashetteTheme.finance.expense else MaterialTheme.colorScheme.primary,
        height = 8.dp,
    )
    Spacer(Modifier.height(10.dp))
    Caption("${hero.spent.toIdr()} of ${hero.limit.toIdr()} spent")
}

@Composable
private fun NetFace(hero: Hero.NetThisMonth, monthLabel: String, onOpenBudget: () -> Unit) {
    val positive = hero.net.signum() >= 0

    Caption("Net this month · $monthLabel")
    Spacer(Modifier.height(6.dp))
    Money(
        text = hero.net.abs().toSignedIdr(negative = !positive),
        spoken = "${hero.net.abs().toSpokenIdr()} ${if (positive) "up" else "down"} this month",
        style = CashetteText.MoneyHero,
        color = if (positive) CashetteTheme.finance.income else CashetteTheme.finance.expense,
    )
    Spacer(Modifier.height(10.dp))
    Caption("${hero.income.toIdr()} in · ${hero.expense.toIdr()} out")
    Spacer(Modifier.height(8.dp))
    // The hero is answering a weaker question than it could. Say so, and offer the fix.
    TextButton(onClick = onOpenBudget, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        Text("Set a budget to see what's left", style = MaterialTheme.typography.labelLarge)
    }
}

/** A month with nothing in it yet. The balance is the only figure that is still true. */
@Composable
private fun AvailableFace(hero: Hero.Available, monthLabel: String, onOpenBudget: () -> Unit) {
    Caption("Available to spend")
    Spacer(Modifier.height(6.dp))
    Money(
        text = hero.total.toIdr(),
        spoken = "${hero.total.toSpokenIdr()} available to spend",
        style = CashetteText.MoneyHero,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(10.dp))
    Caption(
        "across ${hero.accountCount} account${if (hero.accountCount > 1) "s" else ""} · " +
            "nothing recorded in $monthLabel yet",
    )
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onOpenBudget, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        Text("Set a budget for $monthLabel", style = MaterialTheme.typography.labelLarge)
    }
}

// ── Where it's going ─────────────────────────────────────────────────────────

/**
 * Spend per category, with a burn bar when that category has a budget. The bar means
 * two different things, so it looks like two different things: cream against a limit,
 * a muted outline when it is only ranking one category against the biggest.
 */
@Composable
fun CategoryRow(burn: CategoryBurn, topSpend: BigDecimal) {
    val budgeted = burn.limit != null
    val fraction = if (budgeted) burn.fraction else {
        if (topSpend.signum() > 0) (burn.spent.toFloat() / topSpend.toFloat()).coerceIn(0f, 1f) else 0f
    }

    Column(Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = burn.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Money(
                text = burn.spent.toIdr(),
                spoken = burn.spent.toSpokenIdr(),
                style = CashetteText.MoneySmall,
                color = if (burn.over) CashetteTheme.finance.expense else MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(8.dp))
        Rail(
            fraction = fraction,
            fill = when {
                burn.over -> CashetteTheme.finance.expense
                budgeted -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            },
            height = 4.dp,
        )
        if (budgeted) {
            Spacer(Modifier.height(6.dp))
            Caption(
                if (burn.over) "over by ${(burn.spent - burn.limit!!).toIdr()}"
                else "of ${burn.limit!!.toIdr()}",
            )
        }
    }
}

// ── Recent ───────────────────────────────────────────────────────────────────

@Composable
fun TransactionRow(row: TxRow) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Caption(listOf(row.account, row.date).filter { it.isNotBlank() }.joinToString(" · "))
        }
        Spacer(Modifier.width(12.dp))
        Money(
            text = when (row.kind) {
                TxKind.INCOME -> row.amount.toSignedIdr(negative = false)
                TxKind.EXPENSE -> row.amount.toSignedIdr(negative = true)
                TxKind.TRANSFER -> row.amount.toIdr()
            },
            spoken = row.amount.toSpokenIdr(),
            style = CashetteText.MoneySmall,
            color = when (row.kind) {
                TxKind.INCOME -> CashetteTheme.finance.income
                TxKind.EXPENSE -> CashetteTheme.finance.expense
                TxKind.TRANSFER -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

// ── Next bill ────────────────────────────────────────────────────────────────

/**
 * The web relies on a toast you can miss. A bill you have to pay today belongs on the
 * screen, above the analysis, in the accent colour the theme reserves for energy.
 */
@Composable
fun BillRow(bill: Bill, onOpen: () -> Unit) {
    val urgent = bill.urgent
    Surface(
        shape = CashetteShape.Card,
        color = if (urgent) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open subscriptions", onClick = onOpen),
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_subscriptions),
                contentDescription = null,
                tint = if (urgent) MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = bill.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (urgent) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${bill.cycle} · ${bill.dueLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (urgent) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Money(
                text = bill.amount.toIdr(),
                spoken = bill.amount.toSpokenIdr(),
                style = CashetteText.MoneySmall,
                color = if (urgent) MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun Bill.dueLabel(): String = when {
    daysUntil == null -> "no date set"
    daysUntil < 0 -> "overdue by ${-daysUntil}d"
    daysUntil == 0 -> "due today"
    daysUntil == 1 -> "due tomorrow"
    daysUntil <= 7 -> "due in ${daysUntil}d"
    else -> "due in ${daysUntil} days"
}

// ── Accounts ─────────────────────────────────────────────────────────────────

@Composable
fun AccountCardItem(card: AccountCard, modifier: Modifier = Modifier) {
    Surface(
        shape = CashetteShape.Card,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxHeight(),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                painter = painterResource(card.type.accountIcon()),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Money(
                text = card.balance.toIdr(),
                spoken = "${card.name}, ${card.balance.toSpokenIdr()}",
                style = CashetteText.MoneySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun String.accountIcon(): Int = when (this) {
    "bank" -> R.drawable.ic_account_bank
    "ewallet" -> R.drawable.ic_account_ewallet
    "credit" -> R.drawable.ic_account_credit
    else -> R.drawable.ic_account_cash
}

@Composable
fun SpendingTrendCard(
    data: DailyTrendData,
    monthLabel: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val activePoint = selectedIndex?.let { data.points.getOrNull(it) }

    val displayAmount = activePoint?.amount ?: data.totalExpense
    val dateLabel = activePoint?.date?.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))

    Surface(
        shape = CashetteShape.Hero,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = if (activePoint != null) "SPENDING ON $dateLabel".uppercase() else "SPENDING TREND · $monthLabel".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Money(
                        text = displayAmount.toIdr(),
                        spoken = displayAmount.toSpokenIdr(),
                        style = CashetteText.MoneyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Surface(
                    shape = CashetteShape.Pill,
                    color = if (activePoint != null) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = if (activePoint != null) "Day ${activePoint.day}" else "Avg. ${data.averageDaily.toIdr()}/d",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (activePoint != null) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            MinimalistLineChart(
                points = data.points,
                maxAmount = data.maxDaily,
                averageAmount = data.averageDaily,
                selectedIndex = selectedIndex,
                onSelectIndex = { selectedIndex = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "1st",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
                if (data.points.size > 10) {
                    Text(
                        text = "${data.points.size / 2}th",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                Text(
                    text = "${data.points.size}th",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun MinimalistLineChart(
    points: List<DailyPoint>,
    maxAmount: BigDecimal,
    averageAmount: BigDecimal,
    selectedIndex: Int?,
    onSelectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onSelectIndex(findClosestIndex(offset.x, size.width.toFloat(), points.size))
                    },
                    onDrag = { change, _ ->
                        onSelectIndex(findClosestIndex(change.position.x, size.width.toFloat(), points.size))
                    },
                    onDragEnd = { onSelectIndex(null) },
                    onDragCancel = { onSelectIndex(null) },
                )
            }
            .pointerInput(points) {
                detectTapGestures(
                    onPress = { offset ->
                        onSelectIndex(findClosestIndex(offset.x, size.width.toFloat(), points.size))
                        tryAwaitRelease()
                        onSelectIndex(null)
                    },
                )
            },
    ) {
        val width = size.width
        val height = size.height
        if (points.isEmpty() || width <= 0f || height <= 0f) return@Canvas

        val maxVal = maxAmount.toFloat().coerceAtLeast(1f)
        val padTop = 8.dp.toPx()
        val padBottom = 8.dp.toPx()
        val usableHeight = height - padTop - padBottom

        fun getX(index: Int): Float {
            return if (points.size <= 1) width / 2f
            else (index.toFloat() / (points.size - 1)) * width
        }

        fun getY(amount: BigDecimal): Float {
            val ratio = (amount.toFloat() / maxVal).coerceIn(0f, 1f)
            return padTop + (1f - ratio) * usableHeight
        }

        val offsets = points.mapIndexed { idx, p ->
            Offset(getX(idx), getY(p.amount))
        }

        if (averageAmount > BigDecimal.ZERO) {
            val avgY = getY(averageAmount)
            drawLine(
                color = outlineVariant.copy(alpha = 0.35f),
                start = Offset(0f, avgY),
                end = Offset(width, avgY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
        }

        val strokePath = Path()
        val fillPath = Path()

        if (offsets.size == 1) {
            strokePath.moveTo(0f, offsets[0].y)
            strokePath.lineTo(width, offsets[0].y)
            fillPath.moveTo(0f, offsets[0].y)
            fillPath.lineTo(width, offsets[0].y)
            fillPath.lineTo(width, height)
            fillPath.lineTo(0f, height)
            fillPath.close()
        } else {
            strokePath.moveTo(offsets[0].x, offsets[0].y)
            fillPath.moveTo(offsets[0].x, offsets[0].y)

            for (i in 0 until offsets.size - 1) {
                val p0 = offsets[i]
                val p1 = offsets[i + 1]
                val dx = p1.x - p0.x
                val cp1 = Offset(p0.x + dx / 2f, p0.y)
                val cp2 = Offset(p0.x + dx / 2f, p1.y)
                strokePath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                fillPath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
            }

            fillPath.lineTo(offsets.last().x, height)
            fillPath.lineTo(offsets.first().x, height)
            fillPath.close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.22f),
                    primaryColor.copy(alpha = 0.03f),
                    Color.Transparent,
                ),
                startY = padTop,
                endY = height,
            ),
        )

        drawPath(
            path = strokePath,
            color = primaryColor,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        selectedIndex?.let { selIdx ->
            val selOffset = offsets.getOrNull(selIdx)
            if (selOffset != null) {
                drawLine(
                    color = primaryColor.copy(alpha = 0.4f),
                    start = Offset(selOffset.x, 0f),
                    end = Offset(selOffset.x, height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                )

                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    radius = 11.dp.toPx(),
                    center = selOffset,
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 6.dp.toPx(),
                    center = selOffset,
                )
                drawCircle(
                    color = primaryColor,
                    radius = 4.5.dp.toPx(),
                    center = selOffset,
                )
            }
        }
    }
}

private fun findClosestIndex(touchX: Float, width: Float, count: Int): Int {
    if (count <= 1 || width <= 0f) return 0
    val ratio = (touchX / width).coerceIn(0f, 1f)
    return (ratio * (count - 1)).toInt().coerceIn(0, count - 1)
}
