package com.basbasdev.cashette.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

// M3 Expressive moves on springs, not curves. MotionScheme.expressive() drives the
// components; these are for the motion you author yourself, so a hand-rolled animation
// never lands on a different physics than the Button beside it.
//
// Operate mode keeps it short: state change, feedback, reveal. Nothing decorative,
// no orchestrated page-load sequence — the user came here to do a task.
object CashetteMotion {

    /** Press, toggle, selection. Fast and slightly springy; the user is mid-tap. */
    fun <T> fastSpatial() = spring<T>(
        dampingRatio = 0.8f,
        stiffness = 1400f,
    )

    /** The default for anything that moves or resizes: sheets, cards, list reorder. */
    fun <T> defaultSpatial() = spring<T>(
        dampingRatio = 0.75f,
        stiffness = 700f,
    )

    /** Large travel — screen transitions, expanding a summary card into detail. */
    fun <T> slowSpatial() = spring<T>(
        dampingRatio = 0.8f,
        stiffness = 350f,
    )

    /** Colour, alpha, elevation. Effects never overshoot; only geometry does. */
    fun <T> effects() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 1600f,
    )

    /** Typed helpers, because spring() needs a VectorConverter it can resolve. */
    val dpSpatial = spring<Dp>(dampingRatio = 0.75f, stiffness = 700f)
    val floatEffects = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1600f)
    val offsetSpatial = spring<IntOffset>(dampingRatio = 0.75f, stiffness = 700f)
    val sizeSpatial = spring<IntSize>(dampingRatio = 0.8f, stiffness = 500f)

    /** Skeletons and indeterminate loops. Linear, so the loop has no false emphasis. */
    val shimmer = tween<Float>(durationMillis = 1100)

    /** Numbers that count up when a balance changes. Never on first composition. */
    val counter = tween<Float>(durationMillis = 420)
}
