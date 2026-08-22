package com.basbasdev.cashette.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.basbasdev.cashette.R
import com.basbasdev.cashette.feature.transaction.AddTransactionSheet
import com.basbasdev.cashette.feature.transaction.TransactionKind
import com.basbasdev.cashette.ui.theme.CashetteMotion

/**
 * The signed-in shell: bottom bar, add-FAB, snackbars, and the add sheet.
 *
 * Bar and FAB belong to the four top-level destinations only; anything deeper is
 * full-bleed with a back arrow, so depth is legible without reading the title. The
 * sheet is hoisted here rather than into the graph, so it survives a tab switch behind
 * it and is never something the back stack has to model.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppScaffold(
    currentTab: TopLevelDestination?,
    onSelectTab: (TopLevelDestination) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    fabVisible: Boolean = true,
    content: @Composable () -> Unit,
) {
    var fabExpanded by remember { mutableStateOf(false) }
    var pendingKind by remember { mutableStateOf<TransactionKind?>(null) }

    val showsBar = currentTab != null
    val showsFab = currentTab?.showsFab == true

    // Collapsing the menu is what back means while it is open — not leaving the screen.
    BackHandler(enabled = fabExpanded) { fabExpanded = false }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        // The bar brings its own insets and each screen's top app bar brings the status
        // bar; zero here keeps the two from being counted twice.
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Shown and hidden outright, never animated. AnimatedVisibility holds a slot's
        // measured height for the whole exit spring and then drops it in one frame, so
        // the content box below stays short for ~700ms and then grows — which slides
        // anything centred in it downward long after the screen has settled. Toggling
        // instantly means an entering screen is measured at its final height on frame
        // one, and costs nothing: the bar was sliding out underneath a screen that is
        // already animating in, which is two motions competing for the same moment.
        bottomBar = {
            if (showsBar) {
                ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    TopLevelDestination.entries.forEach { destination ->
                        ShortNavigationBarItem(
                            selected = destination == currentTab,
                            onClick = { onSelectTab(destination) },
                            icon = {
                                Icon(
                                    painter = painterResource(destination.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val barHeight = padding.calculateBottomPadding()

        Box(Modifier.fillMaxSize()) {
            // Screens stop above the bar rather than scrolling under it — Operate mode,
            // where a half-clipped row costs more than the flourish is worth. Consuming
            // the padding stops each screen's own Scaffold re-applying the same inset.
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = barHeight)
                    .consumeWindowInsets(padding),
            ) {
                content()
            }

            // A floating FAB sits exactly where a ledger puts its amounts, and it was
            // covering both figures and the section actions beside them. Scrolling down
            // gets it out of the way; scrolling up brings it straight back. Animating it
            // here is safe — unlike the bottom bar, it overlays content rather than
            // occupying a layout slot, so nothing is measured against it.
            if (showsFab) {
                AnimatedVisibility(
                    visible = fabVisible || fabExpanded,
                    enter = scaleIn(CashetteMotion.fastSpatial()) + fadeIn(CashetteMotion.effects()),
                    exit = scaleOut(CashetteMotion.fastSpatial()) + fadeOut(CashetteMotion.effects()),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = barHeight),
                ) {
                    AddFabMenu(
                        expanded = fabExpanded,
                        onExpandedChange = { fabExpanded = it },
                        onPick = { kind ->
                            fabExpanded = false
                            pendingKind = kind
                        },
                    )
                }
            }
        }
    }

    pendingKind?.let { kind ->
        AddTransactionSheet(initialKind = kind, onDismiss = { pendingKind = null })
    }
}

/**
 * Recording money is the app's primary act, so it is a permanent affordance rather than
 * a control the user has to go find. Anchored bottom-end above the bar: M3 has no
 * centre-docked FAB over a navigation bar, and hand-building one would cost the bar its
 * tab semantics.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPick: (TransactionKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButtonMenu(
        expanded = expanded,
        modifier = modifier,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = onExpandedChange,
                // Cream at both ends of the morph. The default steps primaryContainer ->
                // primary, which lands the resting FAB on a muddy khaki; the contract
                // gives primary to the FAB outright.
                containerColor = ToggleFloatingActionButtonDefaults.containerColor(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary,
                ),
                // Half of the 56dp container, held across the morph: Cashette has no
                // square buttons, and the default rounds a square into a squircle.
                containerCornerRadius = ToggleFloatingActionButtonDefaults.containerCornerRadius(
                    28.dp,
                    28.dp,
                ),
                modifier = Modifier.semantics {
                    contentDescription = if (expanded) "Close record menu" else "Record a transaction"
                },
            ) {
                // The plus turns into a close. checkedProgress is already spring-driven by
                // the component, so reading it here animates nothing twice.
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = checkedProgress * 45f },
                )
            }
        },
    ) {
        TransactionKind.entries.forEach { kind ->
            FloatingActionButtonMenuItem(
                onClick = { onPick(kind) },
                icon = { Icon(painterResource(kind.icon), contentDescription = null) },
                text = { Text(kind.label) },
                // Tonal controls are sage here, not the default primaryContainer khaki —
                // the same container the bar's active indicator uses.
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
