package com.basbasdev.cashette.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.basbasdev.cashette.ui.theme.CashetteMotion
import com.basbasdev.cashette.ui.theme.CashetteShape

// Shared across features. Anything used by more than one screen lives here rather than
// being imported out of a sibling feature package.

/** A section title on the ground, with its way out. No card — cards would nest. */
@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
    style: TextStyle,
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
fun Rail(fraction: Float, fill: Color, height: Dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(CashetteShape.Pill)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) },
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
fun Skeleton(width: Dp? = null, height: Dp) {
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Caption(message, Modifier.weight(1f))
        TextButton(onClick = onRetry) {
            Text("Retry", style = MaterialTheme.typography.labelMedium)
        }
    }
}
