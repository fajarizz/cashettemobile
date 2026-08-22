package com.basbasdev.cashette.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * The design system made visible. Not a screen — open it in the Android Studio
 * preview pane to check tone separation, type rhythm, and money colour before
 * building anything on top.
 */
@Composable
private fun Swatch(color: Color, on: Color, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(CashetteShape.Field)
            .background(color)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = on)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun ThemeSpecimen() {
    val scheme = MaterialTheme.colorScheme
    val finance = CashetteTheme.finance

    Surface(color = scheme.background) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text("Net this month", style = MaterialTheme.typography.titleMedium, color = scheme.onSurfaceVariant)
            Text(
                "Rp 2.091.063",
                style = CashetteText.MoneyHero,
                color = scheme.onSurface,
            )

            SectionLabel("SURFACE LAYERS")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    scheme.surfaceContainerLowest,
                    scheme.surfaceContainerLow,
                    scheme.surfaceContainer,
                    scheme.surfaceContainerHigh,
                    scheme.surfaceContainerHighest,
                ).forEach { Swatch(it, scheme.onSurface, "", Modifier.weight(1f).height(48.dp)) }
            }

            SectionLabel("ACCENTS")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Swatch(scheme.primary, scheme.onPrimary, "Primary", Modifier.weight(1f))
                Swatch(scheme.secondary, scheme.onSecondary, "Secondary", Modifier.weight(1f))
                Swatch(scheme.tertiary, scheme.onTertiary, "Tertiary", Modifier.weight(1f))
            }

            SectionLabel("MONEY — NOT THE ERROR ROLE")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Swatch(finance.incomeContainer, finance.onIncomeContainer, "Income", Modifier.weight(1f))
                Swatch(finance.expenseContainer, finance.onExpenseContainer, "Expense", Modifier.weight(1f))
                Swatch(scheme.errorContainer, scheme.onErrorContainer, "Error", Modifier.weight(1f))
            }
            Row(
                Modifier.padding(top = 10.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Salary", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                Text("+Rp 4.912.443", style = CashetteText.MoneySmall, color = finance.income)
            }
            Row(
                Modifier.padding(top = 4.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Groceries", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                Text("-Rp 2.821.380", style = CashetteText.MoneySmall, color = finance.expense)
            }

            SectionLabel("RANKED CATEGORY RAMP")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                finance.chartRamp.forEach {
                    Column(
                        Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(CashetteShape.Field)
                            .background(it),
                    ) {}
                }
            }

            SectionLabel("CONTROLS — EVERY BUTTON IS A PILL")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button({}, shape = CashetteShape.Pill) { Text("Save") }
                FilledTonalButton({}, shape = CashetteShape.Pill) { Text("Later") }
                OutlinedButton({}, shape = CashetteShape.Pill) { Text("Cancel") }
            }

            SectionLabel("TYPE SCALE")
            Text("Cash Flow", style = MaterialTheme.typography.headlineMedium, color = scheme.onSurface)
            Text("Income vs expense over time", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            Text("LAST 6 MONTHS", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)

            SectionLabel("SHAPE")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    CashetteShape.Field to "12",
                    CashetteShape.Card to "16",
                    CashetteShape.Hero to "24",
                ).forEach { (shape, label) ->
                    Column(
                        Modifier
                            .size(64.dp)
                            .clip(shape)
                            .background(scheme.surfaceContainerHigh)
                            .padding(8.dp),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Preview(name = "Cashette", showBackground = true, heightDp = 1100)
@Composable
private fun ThemeSpecimenPreview() {
    CashetteTheme { ThemeSpecimen() }
}
