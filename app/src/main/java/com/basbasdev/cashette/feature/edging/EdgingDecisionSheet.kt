package com.basbasdev.cashette.feature.edging

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.basbasdev.cashette.R
import com.basbasdev.cashette.core.money.toIdr
import com.basbasdev.cashette.core.money.toSpokenIdr
import com.basbasdev.cashette.ui.components.Caption
import com.basbasdev.cashette.ui.components.FormField
import com.basbasdev.cashette.ui.components.Money
import com.basbasdev.cashette.ui.components.MoneyField
import com.basbasdev.cashette.ui.components.toAmountOrNull
import com.basbasdev.cashette.ui.theme.CashetteShape
import com.basbasdev.cashette.ui.theme.CashetteText
import com.basbasdev.cashette.ui.theme.CashetteTheme
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgingDecisionSheet(
    item: EdgingItemModel,
    spendableBalance: BigDecimal,
    onDismiss: () -> Unit,
    onPass: () -> Unit,
    onBuy: (finalPrice: Double, shippingFee: Double, platform: String, buyReason: String?) -> Unit,
) {
    var showBuyForm by remember { mutableStateOf(false) }
    var finalPriceInput by remember { mutableStateOf(item.price.toBigInteger().toString()) }
    var shippingFeeInput by remember { mutableStateOf("0") }
    var platform by remember { mutableStateOf(item.platform) }
    var buyReason by remember { mutableStateOf("") }

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
                text = "Cooldown Complete",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "You've waited 3 days. Do you still want this?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            Surface(
                shape = CashetteShape.Card,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Money(
                        text = item.price.toIdr(),
                        spoken = item.price.toSpokenIdr(),
                        style = CashetteText.MoneyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CashetteShape.Pill,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = item.platform,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        Surface(
                            shape = CashetteShape.Pill,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = item.priority.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Surface(
                shape = CashetteShape.Card,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Caption("Available Spendable Balance")
                        Money(
                            text = spendableBalance.toIdr(),
                            spoken = spendableBalance.toSpokenIdr(),
                            style = CashetteText.MoneyMedium,
                            color = if (spendableBalance >= item.price) CashetteTheme.finance.income
                            else CashetteTheme.finance.expense,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (!showBuyForm) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onPass,
                        shape = CashetteShape.Pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "I'll pass (Resist & Save ${item.price.toIdr()})",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }

                    OutlinedButton(
                        onClick = { showBuyForm = true },
                        shape = CashetteShape.Pill,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) {
                        Text(
                            text = "I still want it (Buy)",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MoneyField(
                        value = finalPriceInput,
                        onValueChange = { finalPriceInput = it },
                        label = "Final Price Paid",
                    )

                    MoneyField(
                        value = shippingFeeInput,
                        onValueChange = { shippingFeeInput = it },
                        label = "Shipping Fee (Ongkir)",
                    )

                    Text(
                        text = "Platform",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

                    FormField(
                        value = buyReason,
                        onValueChange = { buyReason = it },
                        label = "Reason for buying (Optional)",
                        placeholder = "e.g. Needed for work, great discount",
                    )

                    Spacer(Modifier.height(10.dp))

                    val price = finalPriceInput.toAmountOrNull()?.toDouble() ?: item.price.toDouble()
                    val ship = shippingFeeInput.toAmountOrNull()?.toDouble() ?: 0.0

                    Button(
                        onClick = {
                            onBuy(price, ship, platform, buyReason.ifBlank { null })
                        },
                        shape = CashetteShape.Pill,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) {
                        Text(
                            text = "Confirm Purchase",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }

                    OutlinedButton(
                        onClick = { showBuyForm = false },
                        shape = CashetteShape.Pill,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Text("Back", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}
