package com.basbasdev.cashette.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.basbasdev.cashette.R
import com.basbasdev.cashette.core.money.toIdr
import com.basbasdev.cashette.core.money.toSpokenIdr
import com.basbasdev.cashette.data.model.AccountItemDto
import com.basbasdev.cashette.data.model.BalanceResultDto
import com.basbasdev.cashette.data.model.BudgetItemDto
import com.basbasdev.cashette.data.model.ParseDto
import com.basbasdev.cashette.data.model.SubscriptionItemDto
import com.basbasdev.cashette.feature.home.Money
import com.basbasdev.cashette.ui.theme.CashetteShape
import com.basbasdev.cashette.ui.theme.CashetteText
import com.basbasdev.cashette.ui.theme.CashetteTheme
import java.math.BigDecimal

private fun Double.money(): BigDecimal = BigDecimal.valueOf(this)

private val INTENT_LABELS = mapOf(
    "add_expense" to "Record expense",
    "add_income" to "Record income",
    "create_budget" to "Set budget",
    "create_account" to "Create account",
    "transfer" to "Transfer",
    "update_account_balance" to "Update balance",
)

private val ACCOUNT_TYPE_LABELS = mapOf(
    "cash" to "Cash",
    "bank" to "Bank",
    "ewallet" to "E-Wallet",
    "credit" to "Credit card",
    "pocket" to "Pocket",
)

private fun accountIcon(type: String): Int = when (type) {
    "bank" -> R.drawable.ic_account_bank
    "ewallet" -> R.drawable.ic_account_ewallet
    "credit" -> R.drawable.ic_account_credit
    "pocket" -> R.drawable.ic_pockets
    else -> R.drawable.ic_account_cash
}

/**
 * Every card the assistant can attach to a reply. Read-only answers render their result;
 * anything that would move money renders its terms and waits.
 */
@Composable
fun ReplyCard(
    turn: ChatTurn,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenSubscriptions: () -> Unit,
) {
    val parse = turn.parse ?: return

    parse.balanceResult?.let { BalanceCard(it); return }
    parse.accountsResult?.let { ListCard(it.map(::accountEntry), "All accounts", onOpenAccounts); return }
    parse.budgetsResult?.let { ListCard(it.map(::budgetEntry), "All budgets", onOpenBudget); return }
    parse.subscriptionsResult?.let {
        ListCard(it.map(::subscriptionEntry), "All subscriptions", onOpenSubscriptions)
        return
    }

    if (parse.requiresConfirmation) {
        when (parse.intent) {
            "transfer" -> TransferCard(parse, turn, onConfirm, onCancel)
            else -> ConfirmCard(parse, turn, onConfirm, onCancel)
        }
    }
}

// ── Read-only answers ────────────────────────────────────────────────────────

@Composable
private fun BalanceCard(result: BalanceResultDto) {
    CardShell {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(accountIcon(result.accountType)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = result.accountName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = ACCOUNT_TYPE_LABELS[result.accountType] ?: result.accountType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Balance",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Money(
            text = result.balance.money().toIdr(),
            spoken = result.balance.money().toSpokenIdr(),
            style = CashetteText.MoneyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private data class Entry(val label: String, val value: String, val caption: String?, val icon: Int?)

private fun accountEntry(a: AccountItemDto) =
    Entry(a.name, a.balance.money().toIdr(), null, accountIcon(a.accountType))

private fun budgetEntry(b: BudgetItemDto) =
    Entry(b.categoryName, b.monthlyLimit.money().toIdr(), "per month", R.drawable.ic_budget)

private fun subscriptionEntry(s: SubscriptionItemDto) =
    Entry(s.name, s.amount.money().toIdr(), s.billingCycle.replaceFirstChar { it.uppercase() }, R.drawable.ic_subscriptions)

/**
 * The web lays these out as a three-column grid, which on a phone gives each cell about
 * 90dp and truncates every name. A list reads them properly at this width.
 */
@Composable
private fun ListCard(entries: List<Entry>, action: String, onAction: () -> Unit) {
    CardShell {
        entries.forEachIndexed { index, entry ->
            if (index > 0) {
                HorizontalDivider(
                    Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                entry.icon?.let {
                    Icon(
                        painter = painterResource(it),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    entry.caption?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = entry.value,
                    style = CashetteText.MoneySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = onAction,
            shape = CashetteShape.Pill,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(action, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ── Cards that are waiting on the user ───────────────────────────────────────

@Composable
private fun TransferCard(
    parse: ParseDto,
    turn: ChatTurn,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    CardShell(dimmed = turn.cancelled) {
        CardHeader("Transfer", R.drawable.ic_transfer, turn)
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Leg("From", parse.accountName, parse.accountBalance, Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.ic_forward),
                contentDescription = "to",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(16.dp),
            )
            Leg("To", parse.toAccountName, parse.toAccountBalance, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Amount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Money(
            text = parse.amount?.money()?.toIdr() ?: "—",
            spoken = parse.amount?.money()?.toSpokenIdr() ?: "amount unknown",
            style = CashetteText.MoneyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        val fee = parse.tax?.takeIf { it > 0 }
        if (fee != null) {
            Spacer(Modifier.height(10.dp))
            LabelledRow("Fee", "+ ${fee.money().toIdr()}")
            HorizontalDivider(
                Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            LabelledRow(
                "Total deducted",
                ((parse.amount ?: 0.0) + fee).money().toIdr(),
                emphasis = true,
            )
        }

        Actions(turn, onConfirm, onCancel)
    }
}

@Composable
private fun ConfirmCard(
    parse: ParseDto,
    turn: ChatTurn,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val label = INTENT_LABELS[parse.intent] ?: parse.intent.replace('_', ' ')
    val icon = when (parse.intent) {
        "add_income" -> R.drawable.ic_income
        "add_expense" -> R.drawable.ic_expense
        "create_budget" -> R.drawable.ic_budget
        "create_account", "update_account_balance" -> R.drawable.ic_money
        else -> R.drawable.ic_transfer
    }

    val rows = buildList {
        parse.category?.let { add("Category" to it) }
        parse.accountName?.takeIf { parse.intent != "create_account" }?.let { add("Account" to it) }
        parse.toAccountName?.let { add("To" to it) }
        parse.date?.let { add("Date" to it) }
        parse.entityName?.let { add("Name" to it) }
        parse.entityType?.let { add("Type" to (ACCOUNT_TYPE_LABELS[it] ?: it)) }
    }

    CardShell(dimmed = turn.cancelled) {
        CardHeader(label, icon, turn)

        parse.amount?.let {
            Spacer(Modifier.height(14.dp))
            Money(
                text = it.money().toIdr(),
                spoken = it.money().toSpokenIdr(),
                style = CashetteText.MoneyLarge,
                color = when (parse.intent) {
                    "add_income" -> CashetteTheme.finance.income
                    "add_expense" -> CashetteTheme.finance.expense
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }

        if (rows.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            rows.forEach { (label, value) ->
                LabelledRow(label, value)
                Spacer(Modifier.height(6.dp))
            }
        }

        Actions(turn, onConfirm, onCancel)
    }
}

// ── Shared parts ─────────────────────────────────────────────────────────────

@Composable
private fun CardShell(dimmed: Boolean = false, content: @Composable () -> Unit) {
    Surface(
        shape = CashetteShape.Card,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .alpha(if (dimmed) 0.55f else 1f),
    ) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun CardHeader(label: String, icon: Int, turn: ChatTurn) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CashetteShape.Pill,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        when {
            turn.confirmed -> Status("Confirmed", CashetteTheme.finance.income)
            turn.cancelled -> Status("Cancelled", MaterialTheme.colorScheme.onSurfaceVariant)
            turn.fromHistory -> Status("Expired", MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Status(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
}

@Composable
private fun Leg(label: String, name: String?, balance: Double?, modifier: Modifier = Modifier) {
    Surface(
        shape = CashetteShape.Field,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = name ?: "—",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            balance?.let {
                Text(
                    text = it.money().toIdr(),
                    style = CashetteText.MoneySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun LabelledRow(label: String, value: String, emphasis: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (emphasis) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Confirm leads because it is the expected answer, but Cancel is a full-width sibling
 * rather than a text link — this is the last gate before money moves.
 */
@Composable
private fun Actions(turn: ChatTurn, onConfirm: () -> Unit, onCancel: () -> Unit) {
    if (turn.settled) return

    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onCancel,
            shape = CashetteShape.Pill,
            modifier = Modifier.weight(1f),
        ) {
            Text("Cancel", style = MaterialTheme.typography.labelLarge)
        }
        Button(
            onClick = onConfirm,
            shape = CashetteShape.Pill,
            modifier = Modifier.weight(1f),
        ) {
            Text("Confirm", style = MaterialTheme.typography.labelLarge)
        }
    }
}
