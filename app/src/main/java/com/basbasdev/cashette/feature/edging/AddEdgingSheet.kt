package com.basbasdev.cashette.feature.edging

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.basbasdev.cashette.data.model.CategoryDto
import com.basbasdev.cashette.ui.components.FormField
import com.basbasdev.cashette.ui.components.MoneyField
import com.basbasdev.cashette.ui.components.toAmountOrNull
import com.basbasdev.cashette.ui.theme.CashetteShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEdgingSheet(
    categories: List<CategoryDto>,
    onDismiss: () -> Unit,
    onSubmit: (name: String, price: Double, categoryId: String?, priority: Int, platform: String, days: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryDto?>(null) }
    var priority by remember { mutableIntStateOf(2) }
    var platform by remember { mutableStateOf(ECOMMERCE_PLATFORMS.first()) }
    var cooldownDays by remember { mutableIntStateOf(3) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = CashetteShape.Sheet,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Delay a desire",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Put impulse purchases on a 3-day cooling-off timer before spending.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            MoneyField(
                value = priceInput,
                onValueChange = { priceInput = it },
                label = "Price",
            )

            Spacer(Modifier.height(14.dp))

            FormField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                label = "What do you want to buy?",
                placeholder = "e.g. Mechanical Keyboard, Sneakers",
                error = error,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Platform / Store",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ECOMMERCE_PLATFORMS.forEach { p ->
                    FilterChip(
                        selected = platform == p,
                        onClick = { platform = p },
                        label = { Text(p) },
                        shape = CashetteShape.Pill,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Priority",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EdgingPriority.entries.forEach { prio ->
                    FilterChip(
                        selected = priority == prio.value,
                        onClick = { priority = prio.value },
                        label = { Text(prio.label) },
                        shape = CashetteShape.Pill,
                    )
                }
            }

            if (categories.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Category (Optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory?.id == cat.id,
                            onClick = {
                                selectedCategory = if (selectedCategory?.id == cat.id) null else cat
                            },
                            label = { Text(cat.name) },
                            shape = CashetteShape.Pill,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            val parsedPrice = priceInput.toAmountOrNull()?.toDouble() ?: 0.0
            val canSubmit = name.isNotBlank() && parsedPrice > 0

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            error = "Item name is required"
                            return@Button
                        }
                        if (parsedPrice <= 0) {
                            error = "Price must be greater than 0"
                            return@Button
                        }
                        onSubmit(name.trim(), parsedPrice, selectedCategory?.id, priority, platform, cooldownDays)
                    },
                    enabled = canSubmit,
                    shape = CashetteShape.Pill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Text(
                        text = "Start 3-day Cooldown",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = CashetteShape.Pill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Text("Cancel", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}
