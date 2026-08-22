package com.basbasdev.cashette.feature.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.basbasdev.cashette.R
import com.basbasdev.cashette.ui.theme.CashetteShape
import kotlinx.coroutines.delay

/** Mirrors the web's rotating prompts, at the same five-second cadence. */
private val PROMPTS = listOf(
    "Ask anything about your finances…",
    "Bought coffee for 25k",
    "How much did I spend last month?",
    "Show me my biggest expenses",
    "Transfer 200k from BCA to GoPay",
    "Set a budget for groceries",
)

/**
 * Owns the bottom edge of the Chat tab — which is why Chat is the one top-level
 * destination without a FAB. The field grows to a few lines and then scrolls, so a long
 * message never pushes the send button off screen.
 */
@Composable
fun ChatComposer(
    onSend: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    val canSend = enabled && text.isNotBlank()

    Surface(
        shape = CashetteShape.Sheet,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(Modifier.weight(1f).padding(bottom = 10.dp)) {
                if (text.isEmpty()) RotatingPrompt()

                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = LocalTextStyle.current.merge(
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (canSend) {
                                onSend(text)
                                text = ""
                            }
                        },
                    ),
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp),
                )
            }

            Spacer(Modifier.width(8.dp))

            FilledIconButton(
                onClick = {
                    onSend(text)
                    text = ""
                },
                enabled = canSend,
                shape = CashetteShape.Pill,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun RotatingPrompt() {
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            index = (index + 1) % PROMPTS.size
        }
    }

    AnimatedContent(
        targetState = index,
        transitionSpec = {
            (slideInVertically { it / 2 } + fadeIn()) togetherWith
                (slideOutVertically { -it / 2 } + fadeOut())
        },
        label = "prompt",
    ) { i ->
        Text(
            text = PROMPTS[i],
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
