package com.basbasdev.cashette.feature.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.basbasdev.cashette.R
import com.basbasdev.cashette.ui.components.CashetteScreen
import com.basbasdev.cashette.ui.components.EmptyState

/**
 * The signature feature: Cashette is a ledger you talk to. Gets a tab of its own, and
 * is the one top-level destination with no FAB — its composer owns the bottom edge and
 * is already the fastest way to record something.
 */
@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    CashetteScreen(title = "Chat", modifier = modifier) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            EmptyState(
                icon = R.drawable.ic_chat,
                headline = "Tell Cashette what you spent",
                body = "\"Bought coffee for 25k\" is enough — it works out the amount, " +
                    "the category and the account, then asks you to confirm.",
            )
        }
    }
}
