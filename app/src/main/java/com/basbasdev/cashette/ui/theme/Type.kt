package com.basbasdev.cashette.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// cashetteweb runs Figtree Variable. Drop figtree_regular/medium/semibold/bold.ttf into
// res/font and swap this one declaration; every style below inherits it.
val CashetteFontFamily = FontFamily.SansSerif

private val Trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun face(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = CashetteFontFamily,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = tracking.em,
    lineHeightStyle = Trim,
)

// Operate mode: one family, fixed scale, ~1.2 steps. Tracking tightens as size grows.
val CashetteTypography = Typography(
    displayLarge = face(57, 64, FontWeight.Bold, -0.025),
    displayMedium = face(45, 52, FontWeight.Bold, -0.022),
    displaySmall = face(36, 44, FontWeight.Bold, -0.02),

    headlineLarge = face(32, 40, FontWeight.Bold, -0.018),
    headlineMedium = face(28, 36, FontWeight.SemiBold, -0.015),
    headlineSmall = face(24, 32, FontWeight.SemiBold, -0.012),

    titleLarge = face(22, 28, FontWeight.SemiBold, -0.01),
    titleMedium = face(16, 24, FontWeight.SemiBold, 0.0),
    titleSmall = face(14, 20, FontWeight.SemiBold, 0.0),

    bodyLarge = face(16, 24, FontWeight.Normal, 0.0),
    bodyMedium = face(14, 20, FontWeight.Normal, 0.0),
    bodySmall = face(12, 16, FontWeight.Normal, 0.005),

    labelLarge = face(14, 20, FontWeight.SemiBold, 0.0),
    labelMedium = face(12, 16, FontWeight.SemiBold, 0.005),
    labelSmall = face(11, 16, FontWeight.SemiBold, 0.01),
)

// Money is read in columns and compared down the page, so figures must not reflow.
// Mirrors the web's tabular-nums on every balance, total, and chart label.
private fun money(size: Int, lineHeight: Int, weight: FontWeight, tracking: Double) =
    face(size, lineHeight, weight, tracking).copy(fontFeatureSettings = "tnum")

object CashetteText {
    val MoneyHero = money(40, 46, FontWeight.Bold, -0.022)
    val MoneyLarge = money(28, 34, FontWeight.Bold, -0.015)
    val MoneyMedium = money(20, 26, FontWeight.SemiBold, -0.01)
    val MoneySmall = money(15, 20, FontWeight.Medium, 0.0)
}
