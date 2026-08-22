package com.basbasdev.cashette.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Generated from the cashetteweb palette. Ground and card tones gamut-clip onto the
// web's exact oklch values; see CLAUDE.md before changing any of it by hand.

val CashetteDarkColors = darkColorScheme(
    primary = Color(0xFFD2C6A5),
    onPrimary = Color(0xFF373012),
    primaryContainer = Color(0xFF4F4627),
    onPrimaryContainer = Color(0xFFE9E2D1),
    inversePrimary = Color(0xFF685E3E),
    secondary = Color(0xFFC1CB9F),
    onSecondary = Color(0xFF29340C),
    secondaryContainer = Color(0xFF404B22),
    onSecondaryContainer = Color(0xFFE0E5CE),
    tertiary = Color(0xFFFDB968),
    onTertiary = Color(0xFF462A00),
    tertiaryContainer = Color(0xFF663E00),
    onTertiaryContainer = Color(0xFFFFDDBA),
    background = Color(0xFF141500),
    onBackground = Color(0xFFE3E4D5),
    surface = Color(0xFF141500),
    onSurface = Color(0xFFE3E4D5),
    surfaceVariant = Color(0xFF464923),
    onSurfaceVariant = Color(0xFFC8C9A0),
    surfaceTint = Color(0xFFD2C6A5),
    inverseSurface = Color(0xFFE3E4D5),
    inverseOnSurface = Color(0xFF2F3219),
    error = Color(0xFFFFB3AF),
    onError = Color(0xFF680015),
    errorContainer = Color(0xFF920022),
    onErrorContainer = Color(0xFFFFDAD7),
    outline = Color(0xFF919469),
    outlineVariant = Color(0xFF464923),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF383B21),
    surfaceDim = Color(0xFF141500),
    surfaceContainerLowest = Color(0xFF0F0F00),
    surfaceContainerLow = Color(0xFF1B1D00),
    surfaceContainer = Color(0xFF1F2105),
    surfaceContainerHigh = Color(0xFF292C13),
    surfaceContainerHighest = Color(0xFF34371D),
)

// Money has direction; M3 roles do not encode it. The error role stays a UI failure state.
val IncomeDark = Color(0xFF4ADE80)
val OnIncomeDark = Color(0xFF141500)
val IncomeContainerDark = Color(0xFF004D23)
val OnIncomeContainerDark = Color(0xFFB8F0C5)

val ExpenseDark = Color(0xFFFF6467)
val OnExpenseDark = Color(0xFF141500)
val ExpenseContainerDark = Color(0xFF7D1F26)
val OnExpenseContainerDark = Color(0xFFFFDAD7)

// Ranked-category ramp for charts: one hue family, ordered by magnitude, never cycled.
val ChartRampDark = listOf(
    Color(0xFFF4F0E8),
    Color(0xFFE2DAC4),
    Color(0xFFB7AB87),
    Color(0xFF8A9668),
    Color(0xFF707C4F),
    Color(0xFF5E6139),
    Color(0xFF464923),
)
