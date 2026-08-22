package com.basbasdev.cashette.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Money colours. M3 has no role for direction-of-value, and [ColorScheme.error] is a
 * failure state, not an expense — overloading it would paint a normal grocery run as a
 * problem. These carry the web's exact income green and expense red.
 */
@Immutable
data class CashetteFinanceColors(
    val income: Color,
    val onIncome: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val onExpense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val chartRamp: List<Color>,
) {
    /** Signed amounts pick their own colour; nothing else in the app may. */
    fun forAmount(amount: Double): Color = if (amount < 0) expense else income

    /** Ranked categories read down the ramp; never cycle it, fold the tail into Other. */
    fun rank(index: Int): Color = chartRamp[index.coerceIn(0, chartRamp.lastIndex)]
}

private val DarkFinanceColors = CashetteFinanceColors(
    income = IncomeDark,
    onIncome = OnIncomeDark,
    incomeContainer = IncomeContainerDark,
    onIncomeContainer = OnIncomeContainerDark,
    expense = ExpenseDark,
    onExpense = OnExpenseDark,
    expenseContainer = ExpenseContainerDark,
    onExpenseContainer = OnExpenseContainerDark,
    chartRamp = ChartRampDark,
)

private val LocalFinanceColors = staticCompositionLocalOf { DarkFinanceColors }

/**
 * Cashette is dark only, like cashetteweb, which hard-codes `<html className="dark">`.
 * The olive ground is the brand; in a light scheme M3 inverts `primary` to a dark tone
 * of the cream and the green disappears entirely, so there is no light scheme to pick.
 * `Theme.Cashette` in themes.xml matches, so there is no white flash before first frame.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CashetteTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFinanceColors provides DarkFinanceColors) {
        MaterialExpressiveTheme(
            colorScheme = CashetteDarkColors,
            typography = CashetteTypography,
            shapes = CashetteShapes,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

/** `CashetteTheme.finance.income` — the money colours, alongside MaterialTheme.*. */
object CashetteTheme {
    val finance: CashetteFinanceColors
        @Composable @ReadOnlyComposable get() = LocalFinanceColors.current
}
