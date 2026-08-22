package com.basbasdev.cashette.feature.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.basbasdev.cashette.R
import com.basbasdev.cashette.ui.components.CashetteScreen
import com.basbasdev.cashette.ui.components.EmptyState

/**
 * The ledger. Answers "what did I spend on X" — the one question Home's recent-activity
 * card cannot, which is why it holds a tab rather than sitting under a hub.
 */
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    CashetteScreen(title = "History", modifier = modifier) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            EmptyState(
                icon = R.drawable.ic_history,
                headline = "Nothing recorded yet",
                body = "Every transaction lands here, searchable by note, category " +
                    "and account. Add one with the button below.",
            )
        }
    }
}
