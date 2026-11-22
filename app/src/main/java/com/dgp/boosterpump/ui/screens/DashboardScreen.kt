package com.dgp.boosterpump.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dgp.boosterpump.controller.AlarmSeverity
import com.dgp.boosterpump.controller.ControllerSnapshot
import com.dgp.boosterpump.controller.PhaseVoltages
import com.dgp.boosterpump.controller.PumpState
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.SectionCard
import com.dgp.boosterpump.ui.components.StatusBadge
import com.dgp.boosterpump.viewmodel.MonitoringUiState

@Composable
fun DashboardScreen(
    uiState: MonitoringUiState,
    onConnect: () -> Unit,
    onOpenGatewaySettings: () -> Unit,
    onOpenPumps: () -> Unit,
) {
    val snapshot = uiState.snapshot
    val conn = uiState.connectionState

    if (snapshot == null) {
        EmptyConnectionState(
            uiState = uiState,
            onConnect = onConnect,
            onOpenGatewaySettings = onOpenGatewaySettings,
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (conn is ConnectionState.Error) {
            item {
                NoticeBox(
                    text = "Communication error: ${conn.message}. Showing last received readings.",
                    tone = BadgeTone.FAULT,
                )
            }
        } else if (conn is ConnectionState.Connected && conn.partialError != null) {
            item {
                NoticeBox(
                    text = "${conn.partialError} — Check Function Code / Address Offset in Gateway settings.",
                    tone = BadgeTone.WARNING,
                )
            }
        }

        item { PressureAndDriveCard(snapshot) }
        item { AlarmsCard(snapshot) }
        item { PumpsOverviewCard(snapshot.pumps, onOpenPumps) }
        item { ElectricalSupplyCard(snapshot.voltages, snapshot.mainsFrequencyHz) }
        item { FloatersAndControllerCard(snapshot) }
    }
}

@Composable
private fun EmptyConnectionState(
    uiState: MonitoringUiState,
    onConnect: () -> Unit,
    onOpenGatewaySettings: () -> Unit,
) {
    val conn = uiState.connectionState
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (conn) {
            is ConnectionState.Connecting -> {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(16.dp))
                Text("Connecting to ${conn.target}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Establishing read-only Modbus session…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is ConnectionState.Error -> {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text("Communication failed", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    conn.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (conn.retryInSeconds != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Retrying automatically in ${conn.retryInSeconds} s…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onOpenGatewaySettings) {
                    Text("Check Gateway Settings")
                }
            }

            else -> {
                Icon(
                    Icons.Outlined.Cable,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text("DGP BPC02 Monitor (Read-Only)", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                val targetDesc = when (uiState.savedConfig.mode) {
                    DataSourceMode.SIMULATION -> "Configured for Simulation mode (no hardware required)."
                    DataSourceMode.LIVE -> if (uiState.savedConfig.host.isBlank()) {
                        "Live mode selected — enter the Samin ES 487 gateway IP address in Gateway Settings."
                    } else {
                        "Target: ${uiState.savedConfig.host}:${uiState.savedConfig.port} (Unit ID ${uiState.savedConfig.unitId})."
                    }
                }
                Text(
                    targetDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onConnect) { Text("Connect") }
                    OutlinedButton(onClick = onOpenGatewaySettings) { Text("Gateway Settings") }
                }
            }
        }
    }
}

@Composable
private fun PressureAndDriveCard(snapshot: ControllerSnapshot) {
    SectionCard(
        title = "System Pressure & Drive",
        subtitle = "Word 2 (0.01 bar) • Word 10 (raw 0–1600, scale unverified)",
        trailing = {
            val (label, tone) = when (snapshot.driveFailed) {
                true -> "Drive Fault" to BadgeTone.FAULT
                false -> "Drive Normal" to BadgeTone.OK
                null -> "Drive N/A" to BadgeTone.NEUTRAL
            }
            StatusBadge(label, tone)
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    "System Pressure",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = snapshot.pressureBar?.let { "%.2f".format(it) } ?: "—",
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Text(
                        text = "bar",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Drive Frequency (raw)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = snapshot.driveFrequencyRaw?.toString() ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "0–1600 • unit unverified",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Reference scale up to 10 bar (common booster sensor; actual sensor max is on controller)
        snapshot.pressureBar?.let { bar ->
            val progress = (bar / 10.0).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("0 bar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "DR Status: ${formatOnOff(snapshot.drStatus)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("10 bar ref", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AlarmsCard(snapshot: ControllerSnapshot) {
    val alarms = snapshot.alarms
    SectionCard(
        title = "Controller Status & Alarms",
        subtitle = "Word 80 (System Error: ${snapshot.systemErrorWord?.let { "0x%04X".format(it) } ?: "N/A"}) • Bits 25–31",
        trailing = {
            if (alarms.isEmpty()) {
                StatusBadge("No alarms", BadgeTone.OK)
            } else {
                StatusBadge("${alarms.size} active", BadgeTone.FAULT)
            }
        },
        borderColor = if (alarms.isNotEmpty()) MaterialTheme.colorScheme.error else null,
    ) {
        if (alarms.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.CheckCircleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text("No documented fault bits active.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                alarms.forEach { alarm ->
                    NoticeBox(
                        text = "${alarm.label} — ${alarm.detail}",
                        tone = if (alarm.severity == AlarmSeverity.FAULT) BadgeTone.FAULT else BadgeTone.WARNING,
                    )
                }
            }
        }
    }
}

@Composable
private fun PumpsOverviewCard(pumps: List<PumpState>, onOpenPumps: () -> Unit) {
    val running = pumps.count { it.running == true }
    val failed = pumps.count { it.failed == true }
    SectionCard(
        title = "Pumps ($running running)",
        subtitle = if (failed > 0) "$failed pump fault(s) reported" else "Bits 19–24 (status) • Bits 26–31 (fail)",
        trailing = {
            OutlinedButton(onClick = onOpenPumps, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("All I/O")
            }
        },
    ) {
        // Two rows of 3 compact pump tiles
        pumps.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { pump ->
                    PumpTile(pump = pump, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PumpTile(pump: PumpState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Pump ${pump.number}", style = MaterialTheme.typography.titleSmall)
            val (label, tone) = when {
                pump.failed == true -> "FAULT" to BadgeTone.FAULT
                pump.running == true -> "RUN" to BadgeTone.OK
                pump.running == false -> "STOP" to BadgeTone.NEUTRAL
                else -> "N/A" to BadgeTone.NEUTRAL
            }
            StatusBadge(label, tone)
            Text(
                text = when {
                    pump.runTime != null -> "${pump.runTime.hours}h ${"%02d".format(pump.runTime.minutes)}m"
                    pump.hasRunTimeRegisters -> "—"
                    else -> "No timer reg"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ElectricalSupplyCard(voltages: PhaseVoltages, mainsFreqHz: Double?) {
    SectionCard(
        title = "Supply Voltages & Mains Frequency",
        subtitle = "Words 3–8 (Volt) • Word 9 (0.01 Hz)",
        trailing = {
            Text(
                text = mainsFreqHz?.let { "%.2f Hz".format(it) } ?: "— Hz",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
    ) {
        Text("Phase – Neutral", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoltageCell("R – N", voltages.rn, Modifier.weight(1f))
            VoltageCell("S – N", voltages.sn, Modifier.weight(1f))
            VoltageCell("T – N", voltages.tn, Modifier.weight(1f))
        }
        HorizontalDivider()
        Text("Phase – Phase", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoltageCell("R – S", voltages.rs, Modifier.weight(1f))
            VoltageCell("S – T", voltages.st, Modifier.weight(1f))
            VoltageCell("T – R", voltages.tr, Modifier.weight(1f))
        }
    }
}

@Composable
private fun VoltageCell(label: String, volts: Int?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = volts?.let { "$it V" } ?: "— V",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun FloatersAndControllerCard(snapshot: ControllerSnapshot) {
    SectionCard(
        title = "Tank Floaters & Controller",
        subtitle = "Bits 32–33 • Word 0",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Low-Level Floater (Bit 32)", style = MaterialTheme.typography.bodyMedium)
            FloaterBadge(snapshot.floaterLowConnected)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("High-Level Floater (Bit 33)", style = MaterialTheme.typography.bodyMedium)
            FloaterBadge(snapshot.floaterHighConnected)
        }
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Controller LCD Page (Word 0)", style = MaterialTheme.typography.bodyMedium)
            Text(
                snapshot.displayedPage?.toString() ?: "—",
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun FloaterBadge(connected: Boolean?) {
    val (label, tone) = when (connected) {
        true -> "Connected" to BadgeTone.OK
        false -> "Not connected" to BadgeTone.WARNING
        null -> "N/A" to BadgeTone.NEUTRAL
    }
    StatusBadge(label, tone)
}

private fun formatOnOff(value: Boolean?): String = when (value) {
    true -> "ON"
    false -> "OFF"
    null -> "N/A"
}
