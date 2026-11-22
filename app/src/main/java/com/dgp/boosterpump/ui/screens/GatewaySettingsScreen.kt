package com.dgp.boosterpump.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dgp.boosterpump.controller.ControllerModel
import com.dgp.boosterpump.gateway.ConfigValidation
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.gateway.ProtocolMapping
import com.dgp.boosterpump.modbus.Framing
import com.dgp.boosterpump.modbus.ReadFunction
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.SectionCard
import com.dgp.boosterpump.viewmodel.GatewayFormState
import com.dgp.boosterpump.viewmodel.GatewaySettingsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GatewaySettingsScreen(
    form: GatewayFormState,
    viewModel: GatewaySettingsViewModel,
    onSavedAndConnected: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(
                title = "Data Source & Controller Model",
                subtitle = "Simulation mode exercises the full Modbus decoder without physical hardware",
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DataSourceMode.entries.forEach { mode ->
                        FilterChip(
                            selected = form.mode == mode,
                            onClick = { viewModel.updateMode(mode) },
                            label = { Text(mode.label) },
                        )
                    }
                }
                if (form.mode == DataSourceMode.SIMULATION) {
                    NoticeBox(
                        text = "Simulation mode generates synthetic BPC02 readings in memory. A persistent banner is always shown so simulated data is never mistaken for a real controller.",
                        tone = BadgeTone.WARNING,
                    )
                }

                Text("Controller model", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ControllerModel.entries.forEach { model ->
                        FilterChip(
                            selected = form.controllerModel == model,
                            onClick = { viewModel.updateModel(model) },
                            enabled = model.isSupported,
                            label = {
                                Text(if (model.isSupported) model.label else "${model.label} (planned)")
                            },
                        )
                    }
                }
                Text(
                    form.controllerModel.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (form.mode == DataSourceMode.LIVE) {
            item {
                SectionCard(
                    title = "Samin ES 487 Gateway Connection",
                    subtitle = "RS485-to-Ethernet Modbus TCP/IP converter (5–48 VDC, Web UI, Server/Client, DHCP/Static)",
                ) {
                    OutlinedTextField(
                        value = form.host,
                        onValueChange = viewModel::updateHost,
                        label = { Text("Gateway IP address or hostname") },
                        placeholder = { Text("e.g. 192.168.1.50 (use ES 487 IP Finder)") },
                        singleLine = true,
                        isError = ConfigValidation.Field.HOST in form.errors,
                        supportingText = form.errors[ConfigValidation.Field.HOST]?.let { err -> { Text(err) } },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = form.portText,
                            onValueChange = viewModel::updatePort,
                            label = { Text("TCP Port") },
                            supportingText = {
                                Text(form.errors[ConfigValidation.Field.PORT] ?: "Confirm in ES 487 Web UI (often 502)")
                            },
                            singleLine = true,
                            isError = ConfigValidation.Field.PORT in form.errors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = form.unitIdText,
                            onValueChange = viewModel::updateUnitId,
                            label = { Text("Unit / Slave ID") },
                            supportingText = {
                                Text(form.errors[ConfigValidation.Field.UNIT_ID] ?: "BPC02 Network menu (verified: 1)")
                            },
                            singleLine = true,
                            isError = ConfigValidation.Field.UNIT_ID in form.errors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Text("TCP Framing (match ES 487 working mode)", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Framing.entries.forEach { framing ->
                            FilterChip(
                                selected = form.framing == framing,
                                onClick = { viewModel.updateFraming(framing) },
                                label = { Text(framing.label) },
                            )
                        }
                    }
                    Text(
                        form.framing.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    NoticeBox(
                        text = "Configure the Samin ES 487 via its web browser interface: Mode = TCP Server (Modbus TCP ↔ Modbus RTU), RS485 serial port = 9600 baud, 8 data bits, No parity, 1 stop bit (9600 8N1) to match the DGP BPC02.",
                        tone = BadgeTone.NEUTRAL,
                    )
                }
            }
        }

        item {
            SectionCard(
                title = "Modbus Table Mapping (Hardware-Verified on BPC02)",
                subtitle = "BPC02 hardware tests confirmed FC01 (Bits), FC03 (Words), and zero-based offset 0",
            ) {
                NoticeBox(
                    text = "Hardware verified on BPC02: Bit addresses require FC01 (Read Coils) and Word addresses require FC03 (Read Holding Registers) with Offset 0. " +
                        "FC02 and FC04 are rejected by BPC02 firmware with non-standard 4-byte frames (01 02 81 E1 / 01 04 01 E3) — keep FC01 + FC03 + Offset 0 selected for BPC02.",
                    tone = BadgeTone.OK,
                )

                Text("Bit Address table (0–33)", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ReadFunction.READ_COILS, ReadFunction.READ_DISCRETE_INPUTS).forEach { fn ->
                        FilterChip(
                            selected = form.bitReadFunction == fn,
                            onClick = { viewModel.updateBitFunction(fn) },
                            label = { Text(fn.label) },
                        )
                    }
                }

                Text("Word Address table (0–80)", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ReadFunction.READ_HOLDING_REGISTERS, ReadFunction.READ_INPUT_REGISTERS).forEach { fn ->
                        FilterChip(
                            selected = form.wordReadFunction == fn,
                            onClick = { viewModel.updateWordFunction(fn) },
                            label = { Text(fn.label) },
                        )
                    }
                }

                Text("PDU Address Offset", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProtocolMapping.ALLOWED_OFFSETS.forEach { offset ->
                        val label = when (offset) {
                            0 -> "0 (printed = PDU)"
                            -1 -> "-1 (1-based → 0-based)"
                            else -> "+1"
                        }
                        FilterChip(
                            selected = form.addressOffset == offset,
                            onClick = { viewModel.updateAddressOffset(offset) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Polling & Timeouts",
                subtitle = "Keep poll interval ≥ 1000 ms to avoid saturating the 9600 bps RS485 bus",
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = form.pollIntervalText,
                        onValueChange = viewModel::updatePollInterval,
                        label = { Text("Poll interval (ms)") },
                        singleLine = true,
                        isError = ConfigValidation.Field.POLL_INTERVAL in form.errors,
                        supportingText = form.errors[ConfigValidation.Field.POLL_INTERVAL]?.let { e -> { Text(e) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    if (form.mode == DataSourceMode.LIVE) {
                        OutlinedTextField(
                            value = form.responseTimeoutText,
                            onValueChange = viewModel::updateResponseTimeout,
                            label = { Text("Response timeout (ms)") },
                            singleLine = true,
                            isError = ConfigValidation.Field.RESPONSE_TIMEOUT in form.errors,
                            supportingText = form.errors[ConfigValidation.Field.RESPONSE_TIMEOUT]?.let { e -> { Text(e) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (form.mode == DataSourceMode.LIVE) {
                    OutlinedTextField(
                        value = form.connectTimeoutText,
                        onValueChange = viewModel::updateConnectTimeout,
                        label = { Text("TCP connect timeout (ms)") },
                        singleLine = true,
                        isError = ConfigValidation.Field.CONNECT_TIMEOUT in form.errors,
                        supportingText = form.errors[ConfigValidation.Field.CONNECT_TIMEOUT]?.let { e -> { Text(e) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                form.statusMessage?.let { msg ->
                    NoticeBox(text = msg, tone = BadgeTone.OK)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { viewModel.save(andConnect = true, onConnected = onSavedAndConnected) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save & Connect")
                    }
                    OutlinedButton(
                        onClick = { viewModel.save(andConnect = false) },
                        modifier = Modifier.weight(0.7f),
                    ) {
                        Text("Save Only")
                    }
                }
            }
        }
    }
}
