package com.basbasdev.cashette.feature.transaction

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.basbasdev.cashette.R
import com.basbasdev.cashette.ui.theme.CashetteShape

/**
 * The three things the FAB can record. Order and default match cashetteweb's tabs in
 * `components/add-transaction-modal.tsx`, which opens on Expense.
 */
enum class TransactionKind(val label: String, @DrawableRes val icon: Int) {
    EXPENSE("Expense", R.drawable.ic_expense),
    INCOME("Income", R.drawable.ic_income),
    TRANSFER("Transfer", R.drawable.ic_transfer),
}

/**
 * Hosted by [com.basbasdev.cashette.navigation.AppScaffold], above the NavHost, so it
 * composes over whichever tab is showing and is not itself a destination.
 *
 * The kind picked from the FAB menu only seeds the sheet — switching here is one tap,
 * the same as switching tabs in the web modal.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddTransactionSheet(
    initialKind: TransactionKind,
    onDismiss: () -> Unit,
) {
    var kind by remember { mutableStateOf(initialKind) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = CashetteShape.Sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = "Record",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionKind.entries.forEach { option ->
                    FilledTonalToggleButton(
                        checked = kind == option,
                        onCheckedChange = { kind = option },
                        // The pill squashes on press and springs back — the detail that
                        // sells an Expressive control. CashetteShape owns both shapes.
                        shapes = ToggleButtonShapes(
                            shape = CashetteShape.Pill,
                            pressedShape = CashetteShape.PillPressed,
                            checkedShape = CashetteShape.Pill,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            painter = painterResource(option.icon),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(option.label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            // TODO: amount, account, category and date fields, then POST /api/transactions
            //  and invalidate the affected repositories. The web reloads the window here;
            //  this app has repositories to invalidate instead.
            Text(
                text = "The ${kind.label.lowercase()} form lands with the data layer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
