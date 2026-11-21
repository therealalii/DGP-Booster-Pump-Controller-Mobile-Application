package com.dgp.boosterpump.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dgp.boosterpump.modbus.ReadFunction
import com.dgp.boosterpump.modbus.TransactionRecord
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.SectionCard
import com.dgp.boosterpump.ui.components.StatusBadge
import com.dgp.boosterpump.ui.components.formatTime
import com.dgp.boosterpump.viewmodel.MonitoringUiState

/**
 * Hardware-commissioning tools: a read-only single-request probe (FC01–FC04) to verify
 * BPC02 function codes, PDU offsets and Drive Frequency scaling against the physical LCD,
 * plus a live raw PDU transaction log.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagnosticsScreen(
    uiState: MonitoringUiState,
    onRunProbe: (ReadFunction, Int, Int) -> Unit,
    onClearLog: () -> Unit,
) {
    var selectedFn by remember { mutableStateOf(ReadFunction.READ_HOLDING_REGISTERS) }
    var addrText by remember { mutableStateOf("2") } // Word 2 = System pressure
    var qtyText by remember { mutableStateOf("1") }
    var inputError by remember { mutableStateOf<String?>(null) }

    val isConnected = uiState.connectionState is ConnectionState.Connected

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(
                title = "Read-Only Register Probe (Hardware Verification)",
                subtitle = "Test FC01/FC02 and FC03/FC04 directly against the controller LCD. Write functions are not available.",
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ReadFunction.entries.forEach { fn ->
                        FilterChip(
                            selected = fn == selectedFn,
                            onClick = { selectedFn = fn },
                            label = { Text("FC%02X".format(fn.code)) },
                        )
                    }
                }
                Text(
                    selectedFn.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Quick presets from DGP_BPC02_Modbus_Research_Findings.txt §8
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = {
                            selectedFn = ReadFunction.READ_HOLDING_REGISTERS
                            addrText = "2"
                            qtyText = "1"
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) { Text("Pressure (W2)") }
                    OutlinedButton(
                        onClick = {
                            selectedFn = ReadFunction.READ_HOLDING_REGISTERS
                            addrText = "9"
                            qtyText = "2"
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) { Text("Freq & Drive (W9–10)") }
                    OutlinedButton(
                        onClick = {
                            selectedFn = ReadFunction.READ_COILS
                            addrText = "19"
                            qtyText = "6"
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) { Text("Pumps 1–6 (B19)") }
                    OutlinedButton(
                        onClick = {
                            selectedFn = ReadFunction.READ_HOLDING_REGISTERS
                            addrText = "80"
                            qtyText = "1"
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) { Text("Errors (W80)") }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = addrText,
                        onValueChange = { addrText = it; inputError = null },
                        label = { Text("PDU Address (0-based)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it; inputError = null },
                        label = { Text("Quantity") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.7f),
                    )
                }

                if (inputError != null) {
                    Text(inputError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        val addr = addrText.trim().toIntOrNull()
                        val qty = qtyText.trim().toIntOrNull()
                        when {
                            addr == null || addr !in 0..65535 -> inputError = "PDU address must be 0–65535"
                            qty == null || qty !in 1..selectedFn.maxQuantity ->
                                inputError = "Quantity must be 1–${selectedFn.maxQuantity}"
                            addr + qty > 65536 -> inputError = "Address + quantity exceeds 65535"
                            else -> {
                                inputError = null
                                onRunProbe(selectedFn, addr, qty)
                            }
                        }
                    },
                    enabled = isConnected,
                ) {
                    Text("Send Read Request")
                }

                uiState.probeResult?.let { res ->
                    HorizontalDivider()
                    if (res.error != null) {
                        NoticeBox(
                            text = "FC%02X @ %d (qty %d) → %s".format(res.function.code, res.pduAddress, res.quantity, res.error),
                            tone = BadgeTone.FAULT,
                        )
                    } else if (res.values != null) {
                        val formatted = res.values.mapIndexed { idx, v ->
                            "[%d]=%d (0x%04X)".format(res.pduAddress + idx, v, v)
                        }.joinToString(", ")
                        NoticeBox(
                            text = "${formatTime(res.timestampMillis)} • FC%02X @ %d: %s".format(
                                res.function.code,
                                res.pduAddress,
                                formatted,
                            ),
                            tone = BadgeTone.OK,
                        )
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Modbus Transaction Log (${uiState.transactions.size})",
                subtitle = "Raw request/response PDUs (most recent first)",
                trailing = {
                    TextButton(onClick = onClearLog, enabled = uiState.transactions.isNotEmpty()) {
                        Text("Clear")
                    }
                },
            ) {
                if (uiState.transactions.isEmpty()) {
                    Text(
                        "No transactions recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(uiState.transactions, key = { "${it.timestampMillis}-${it.function.code}-${it.pduAddress}-${it.durationMs}" }) { tx ->
            TransactionCard(tx)
        }
    }
}

@Composable
private fun TransactionCard(tx: TransactionRecord) {
    SectionCard(
        title = "${formatTime(tx.timestampMillis)}  •  Unit ${tx.unitId}  •  FC%02X @ %d (×%d)".format(
            tx.function.code,
            tx.pduAddress,
            tx.quantity,
        ),
        trailing = {
            if (tx.error == null) {
                StatusBadge("${tx.durationMs} ms", BadgeTone.OK)
            } else {
                StatusBadge("ERR", BadgeTone.FAULT)
            }
        },
    ) {
        Text(
            "TX: ${tx.requestHex}",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
        )
        tx.responseHex?.let { rx ->
            Text(
                "RX: $rx",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
            )
        }
        tx.error?.let { err ->
            Text(
                err,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
