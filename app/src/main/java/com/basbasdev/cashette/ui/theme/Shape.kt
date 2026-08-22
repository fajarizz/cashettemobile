package com.basbasdev.cashette.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// The web's radii, one step softer than stock M3 because Cashette's cards are rounder
// than Material's defaults: xl=12 inputs, 2xl=16 cards, 3xl=24 hero, full=pill buttons.
val CashetteShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object CashetteShape {
    /** Text fields, chips, list rows — the web's rounded-xl. */
    val Field = RoundedCornerShape(12.dp)

    /** The default card. Every panel on every screen. The web's rounded-2xl. */
    val Card = RoundedCornerShape(16.dp)

    /** Hero and summary surfaces that need to sit above the card layer. rounded-3xl. */
    val Hero = RoundedCornerShape(24.dp)

    /** Bottom sheets and dialogs. */
    val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** Every button, pill, and FAB. The web has no square buttons and neither do we. */
    val Pill = RoundedCornerShape(percent = 50)

    /** Pressed state for pill controls — the squash that sells an Expressive press. */
    val PillPressed = RoundedCornerShape(CornerSize(14.dp))

    /** Grouped list: first and last rows round outward, middles stay tight. */
    fun groupTop(radius: Int = 16) =
        RoundedCornerShape(topStart = radius.dp, topEnd = radius.dp, bottomStart = 4.dp, bottomEnd = 4.dp)

    fun groupMiddle() = RoundedCornerShape(4.dp)

    fun groupBottom(radius: Int = 16) =
        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = radius.dp, bottomEnd = radius.dp)
}
