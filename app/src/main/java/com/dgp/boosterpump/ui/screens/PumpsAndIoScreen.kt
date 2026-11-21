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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dgp.boosterpump.controller.PumpState
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.SectionCard
import com.dgp.boosterpump.ui.components.StatusBadge
import com.dgp.boosterpump.viewmodel.MonitoringUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PumpsAndIoScreen(uiState: MonitoringUiState) {
    val snapshot = uiState.snapshot
    if (snapshot == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No controller data yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Connect to a gateway or start Simulation mode from the Dashboard.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(
                title = "Pump Operating Status & Run Times",
                subtitle = "Run-time registers exist for Pumps 1–4 (Words 34–45); status/fail bits cover Pumps 1–6.",
            ) {
                snapshot.pumps.forEachIndexed { idx, pump ->
                    if (idx > 0) HorizontalDivider()
                    PumpDetailRow(pump)
                }
            }
        }

        item {
            SectionCard(
                title = "Drive & Tank Floater Inputs",
                subtitle = "Bit 10 (DR) • Bit 25 (Drive Fail) • Bits 32–33 (Floaters)",
            ) {
                StatusLine("DR Status (Bit 10)", snapshot.drStatus, "ON", "OFF", BadgeTone.OK, BadgeTone.NEUTRAL)
                StatusLine("Drive Fail (Bit 25)", snapshot.driveFailed, "FAILED", "Normal", BadgeTone.FAULT, BadgeTone.OK)
                StatusLine("Floater Low Level (Bit 32)", snapshot.floaterLowConnected, "Connected", "Not connected", BadgeTone.OK, BadgeTone.WARNING)
                StatusLine("Floater High Level (Bit 33)", snapshot.floaterHighConnected, "Connected", "Not connected", BadgeTone.OK, BadgeTone.WARNING)
            }
        }

        item {
            SectionCard(
                title = "Digital Outputs (Bits 0–9)",
                subtitle = "Controller relay output statuses (read-only)",
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    snapshot.outputs.forEachIndexed { index, state ->
                        IoChip(label = "OUT $index", active = state)
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Feedback & Multi-Function Inputs",
                subtitle = "PR1–PR4 direct-contactor feedback (Bits 11–14) • MFI1–MFI4 configurable inputs (Bits 15–18)",
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    snapshot.prInputs.forEachIndexed { index, state ->
                        IoChip(label = "PR${index + 1}", active = state)
                    }
                    snapshot.mfiInputs.forEachIndexed { index, state ->
                        IoChip(label = "MFI${index + 1}", active = state)
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Manual Control — Locked (Safety)",
                subtitle = "Multi Function Register 99 • Manual Bits 50–59 • Analog Speed Words 50–51",
                trailing = { StatusBadge("DISABLED", BadgeTone.WARNING) },
            ) {
                NoticeBox(
                    text = "Write commands are disabled in this build. The BPC-02 manual does not confirm the Modbus table " +
                        "or write function codes (FC05/FC0F/FC06/FC10) for manual mode (address 99), manual outputs (50–59), " +
                        "or drive speed (50–51). Manual controls remain locked until DGP confirms the protocol and engineering approves bench-tested writes.",
                    tone = BadgeTone.WARNING,
                )
            }
        }
    }
}

@Composable
private fun PumpDetailRow(pump: PumpState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Pump ${pump.number}", style = MaterialTheme.typography.titleSmall)
            val runTimeText = when {
                pump.runTime != null -> "Run time: ${pump.runTime.format()}"
                pump.hasRunTimeRegisters -> "Run time: unavailable"
                else -> "Run time: not documented for Pump ${pump.number}"
            }
            Text(runTimeText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val (runLabel, runTone) = when (pump.running) {
                true -> "RUN" to BadgeTone.OK
                false -> "STOP" to BadgeTone.NEUTRAL
                null -> "N/A" to BadgeTone.NEUTRAL
            }
            StatusBadge(runLabel, runTone)
            if (pump.failed == true) {
                StatusBadge("FAULT", BadgeTone.FAULT)
            }
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: Boolean?,
    trueText: String,
    falseText: String,
    trueTone: BadgeTone,
    falseTone: BadgeTone,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        val (text, tone) = when (value) {
            true -> trueText to trueTone
            false -> falseText to falseTone
            null -> "N/A" to BadgeTone.NEUTRAL
        }
        StatusBadge(text, tone)
    }
}

@Composable
private fun IoChip(label: String, active: Boolean?) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            val (text, tone) = when (active) {
                true -> "ON" to BadgeTone.OK
                false -> "OFF" to BadgeTone.NEUTRAL
                null -> "—" to BadgeTone.NEUTRAL
            }
            StatusBadge(text, tone)
        }
    }
}
