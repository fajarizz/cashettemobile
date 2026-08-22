package com.basbasdev.cashette.feature.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.basbasdev.cashette.R
import com.basbasdev.cashette.core.money.toIdr
import com.basbasdev.cashette.core.money.toSignedIdr
import com.basbasdev.cashette.core.money.toSpokenIdr
import com.basbasdev.cashette.ui.theme.CashetteMotion
import com.basbasdev.cashette.ui.theme.CashetteShape
import com.basbasdev.cashette.ui.theme.CashetteText
import com.basbasdev.cashette.ui.theme.CashetteTheme
import java.math.BigDecimal

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

// ── Shared parts ─────────────────────────────────────────────────────────────

/** A section title on the ground, with its way out. No card — cards would nest. */
@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * Every money figure carries a spoken form. Read literally, "Rp 2.610.000" comes out of
 * TalkBack as "R P two point six one zero point zero zero zero".
 */
@Composable
fun Money(
    text: String,
    spoken: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
    )
}

@Composable
fun Rail(fraction: Float, fill: Color, height: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(CashetteShape.Pill)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(CashetteShape.Pill)
                    .background(fill),
            )
        }
    }
}

/** Skeletons shaped like the thing they stand in for. Never a spinner parked in content. */
@Composable
fun Skeleton(width: androidx.compose.ui.unit.Dp? = null, height: androidx.compose.ui.unit.Dp) {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(CashetteMotion.shimmer, RepeatMode.Reverse),
        label = "pulse",
    )
    Box(
        Modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(CashetteShape.Field)
            .alpha(pulse)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

/** A section that failed on its own. One retry, in place, not a whole-screen apology. */
@Composable
fun SectionError(message: String, onRetry: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Caption(message, Modifier.weight(1f))
        TextButton(onClick = onRetry) {
            Text("Retry", style = MaterialTheme.typography.labelMedium)
        }
    }
}
