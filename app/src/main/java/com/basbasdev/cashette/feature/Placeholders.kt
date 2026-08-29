package com.basbasdev.cashette.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.basbasdev.cashette.R
import com.basbasdev.cashette.auth.ui.components.AuthTextLink
import com.basbasdev.cashette.ui.components.CashetteScreen
import com.basbasdev.cashette.ui.components.EmptyState

// The seven destinations one level below the bottom bar. Each carries its real top app
// bar, back arrow, insets and empty state, so the navigation shell is verifiable before
// any of them have data — and each moves to its own feature/<name>/ file as it is built.

@Composable
fun AnalyticsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) = Placeholder(
    title = "Analytics",
    icon = R.drawable.ic_analytics,
    headline = "Not enough history yet",
    body = "Once a few weeks are recorded, this compares periods, ranks categories " +
        "and shows where the money actually goes.",
    onBack = onBack,
    modifier = modifier,
)




@Composable
private fun Placeholder(
    title: String,
    icon: Int,
    headline: String,
    body: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CashetteScreen(title = title, onBack = onBack, modifier = modifier) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            EmptyState(icon = icon, headline = headline, body = body)
        }
    }
}
