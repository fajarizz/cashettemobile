package com.basbasdev.cashette.feature.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basbasdev.cashette.R
import com.basbasdev.cashette.feature.home.Section
import com.basbasdev.cashette.feature.home.dataOrNull
import com.basbasdev.cashette.ui.components.AddCard
import com.basbasdev.cashette.ui.components.CashetteScreen
import com.basbasdev.cashette.ui.components.ConfirmDialog
import com.basbasdev.cashette.ui.components.EmptyState
import com.basbasdev.cashette.ui.components.FormField
import com.basbasdev.cashette.ui.components.FormSheet
import com.basbasdev.cashette.ui.components.MoneyField
import com.basbasdev.cashette.ui.components.PickerField
import com.basbasdev.cashette.ui.components.SectionError
import com.basbasdev.cashette.ui.components.toAmountOrNull

/**
 * A pocket is money set aside *inside* a real account, so it never adds to net worth —
 * it only reserves part of a balance that is already counted.
 */
@Composable
fun PocketsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoneyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Holding?>(null) }

    val parents = state.accounts.dataOrNull.orEmpty()

    CashetteScreen(title = "Pockets", onBack = onBack, modifier = modifier) { padding ->
        when (val section = state.pockets) {
            is Section.Loading -> LoadingList(padding)

            is Section.Failed -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                SectionError(section.message, viewModel::load)
            }

            is Section.Data -> if (section.value.isEmpty()) {
                EmptyState(
                    icon = R.drawable.ic_pockets,
                    headline = "No pockets yet",
                    body = "A pocket sets money aside inside an account — an emergency " +
                        "fund, a trip — with a target to work towards.",
                    modifier = Modifier.padding(padding),
                    action = {
                        if (parents.isNotEmpty()) AddCard("Add a pocket") { creating = true }
                    },
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding() + 12.dp,
                        bottom = padding.calculateBottomPadding() + 32.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(section.value, key = { it.id }) { pocket ->
                        HoldingRow(
                            holding = pocket,
                            caption = pocket.parentName?.let { "inside $it" } ?: "Pocket",
                            onClick = { deleting = pocket },
                            onLongClick = { deleting = pocket },
                        )
                    }
                    if (parents.isNotEmpty()) {
                        item { AddCard("Add a pocket") { creating = true } }
                    }
                }
            }
        }
    }

    if (creating) {
        var name by remember { mutableStateOf("") }
        var parent by remember { mutableStateOf(parents.firstOrNull()) }
        var target by remember { mutableStateOf("") }
        var nameError by remember { mutableStateOf<String?>(null) }

        FormSheet(
            title = "New pocket",
            submitLabel = "Add pocket",
            submitting = state.working,
            error = state.formError,
            onDismiss = { creating = false; viewModel.clearFormError() },
            onSubmit = {
                val chosen = parent
                if (name.isBlank()) {
                    nameError = "Give the pocket a name."
                } else if (chosen != null) {
                    viewModel.addPocket(name.trim(), chosen.id, target.toAmountOrNull())
                    creating = false
                }
            },
        ) {
            FormField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = "Name",
                placeholder = "Emergency fund",
                error = nameError,
            )
            PickerField(
                label = "Inside which account",
                options = parents,
                selected = parent,
                onSelect = { parent = it },
                optionLabel = { it.name },
            )
            MoneyField(
                value = target,
                onValueChange = { target = it },
                label = "Target (optional)",
                imeAction = ImeAction.Done,
            )
        }
    }

    deleting?.let { pocket ->
        ConfirmDialog(
            title = "Delete ${pocket.name}?",
            body = "The money stays in ${pocket.parentName ?: "its account"} — only the " +
                "pocket goes away.",
            onConfirm = { viewModel.removeAccount(pocket.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}
