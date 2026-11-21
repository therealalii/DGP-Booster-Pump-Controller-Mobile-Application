package com.dgp.boosterpump.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dgp.boosterpump.controller.RegisterReading
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.RegisterDefinition
import com.dgp.boosterpump.controller.register.Verification
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.SectionCard
import com.dgp.boosterpump.ui.components.StatusBadge
import com.dgp.boosterpump.ui.components.formatTime
import com.dgp.boosterpump.viewmodel.MonitoringUiState

@Composable
fun ControllerDetailsScreen(
    uiState: MonitoringUiState,
    onReadHardwareConfig: () -> Unit,
) {
    val active = uiState.activeConfig ?: uiState.savedConfig
    val snapshot = uiState.snapshot
    val cfgSnap = uiState.configSnapshot
    val isConnected = uiState.connectionState is ConnectionState.Connected

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(
                title = "Controller & Link Information",
                subtitle = "Hardware architecture and verified serial parameters",
            ) {
                InfoRow("Controller model", active.controllerModel.label)
                InfoRow("Controller serial (documented)", "Modbus RTU over RS485, 9600 8N1")
                InfoRow("Target gateway", "ENTES ETMO-10 (Ethernet ↔ RS485)")
                InfoRow("TCP framing", active.framing.label)
                InfoRow("Bit read function", active.mapping.bitReadFunction.label)
                InfoRow("Word read function", active.mapping.wordReadFunction.label)
                InfoRow("PDU address offset", "%+d".format(active.mapping.addressOffset))
                InfoRow("Displayed LCD page (Word 0)", snapshot?.displayedPage?.toString() ?: "—")
            }
        }

        item {
            SectionCard(
                title = "Hardware Configuration Registers (On-Demand, Read-Only)",
                subtitle = "Addresses 4096–4110 and 4173–4195. Never polled automatically; never written.",
                trailing = {
                    Button(
                        onClick = onReadHardwareConfig,
                        enabled = isConnected && !cfgSnap.loading,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        if (cfgSnap.loading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("Read once")
                    }
                },
            ) {
                NoticeBox(
                    text = "Manual warning: addresses from row 25 onward are hardware settings; incorrect writes can damage the booster pump. " +
                        "Addresses 4178 ('417B') and 4180 ('41B0') have print/OCR ambiguities in the manual and are marked Ambiguous.",
                    tone = BadgeTone.WARNING,
                )
                if (cfgSnap.timestampMillis != null) {
                    Text(
                        "Last read at ${formatTime(cfgSnap.timestampMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (cfgSnap.errors.isNotEmpty()) {
                    NoticeBox(
                        text = "Some addresses returned exceptions: ${cfgSnap.errors.take(3).joinToString("; ")}",
                        tone = BadgeTone.FAULT,
                    )
                }
                if (cfgSnap.readings.isEmpty()) {
                    Text(
                        if (isConnected) "Press 'Read once' to query configuration registers."
                        else "Connect to a gateway or simulation to read configuration registers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    cfgSnap.readings.forEachIndexed { idx, reading ->
                        if (idx > 0) HorizontalDivider()
                        RegisterRow(reading)
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Polled Register Map (${Bpc02RegisterMap.polled.size} entries)",
                subtitle = "Centralised BPC02 map with raw values, engineering values and verification status",
            ) {
                val rows = snapshot?.readings ?: Bpc02RegisterMap.polled.map { RegisterReading(it, null) }
                rows.forEachIndexed { idx, reading ->
                    if (idx > 0) HorizontalDivider()
                    RegisterRow(reading)
                }
            }
        }

        item {
            SectionCard(
                title = "Locked Control Registers (${Bpc02RegisterMap.manualControl.size} entries)",
                subtitle = "Documented in manual, disabled until write function codes are verified & approved",
                trailing = { StatusBadge("LOCKED", BadgeTone.WARNING) },
            ) {
                Bpc02RegisterMap.manualControl.forEachIndexed { idx, def ->
                    if (idx > 0) HorizontalDivider()
                    LockedRegisterRow(def)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RegisterRow(reading: RegisterReading) {
    val def = reading.definition
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(def.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(
                text = reading.raw?.let { def.format(it) } ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${def.table.name} ${def.documentedAddress}" +
                    (reading.raw?.let { " • raw=$it (0x%04X)".format(it) } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val tone = when (def.verification) {
                Verification.HARDWARE_VERIFIED -> BadgeTone.OK
                Verification.DOCUMENTED -> BadgeTone.NEUTRAL
                Verification.AMBIGUOUS -> BadgeTone.WARNING
            }
            StatusBadge(def.verification.label, tone)
        }
        if (def.notes != null) {
            Text(def.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LockedRegisterRow(def: RegisterDefinition) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(def.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "${def.table.name} ${def.documentedAddress}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (def.notes != null) {
            Text(def.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
