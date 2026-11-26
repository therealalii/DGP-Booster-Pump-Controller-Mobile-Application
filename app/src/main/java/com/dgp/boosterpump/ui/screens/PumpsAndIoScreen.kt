package com.dgp.boosterpump.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Input
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.controller.Bpc02Catalog
import com.dgp.boosterpump.controller.PumpState
import com.dgp.boosterpump.ui.components.AnimatedImpellerIcon
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.LtrText
import com.dgp.boosterpump.ui.components.ParameterHelpBottomSheet
import com.dgp.boosterpump.ui.components.ParameterHelpIcon
import com.dgp.boosterpump.ui.components.PlcLedBead
import com.dgp.boosterpump.ui.components.ScadaPanel
import com.dgp.boosterpump.ui.components.StatusBadge
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.ui.theme.MonoStyle
import com.dgp.boosterpump.viewmodel.MonitoringUiState

@Composable
fun PumpsAndIoScreen(uiState: MonitoringUiState) {
    val p = LocalScadaPalette.current
    val snapshot = uiState.snapshot
    var activeHelp by remember { mutableStateOf<Pair<Bpc02Catalog.ParameterHelp, String?>?>(null) }

    activeHelp?.let { (help, currentVal) ->
        ParameterHelpBottomSheet(
            help = help,
            currentValue = currentVal,
            onDismiss = { activeHelp = null },
        )
    }

    if (snapshot == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(p.panel)
                    .border(0.9.dp, p.gold.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = p.gold, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("اطلاعاتی دریافت نشده است", style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text(
                "ابتدا از صفحه خانه به کنترلر یا حالت نمایشی متصل شوید.",
                style = MaterialTheme.typography.bodySmall,
                color = p.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val pumps = snapshot.visiblePumps
    val activePumps = snapshot.runningPumpCount
    val enabledPumps = snapshot.enabledPumpCount
    val failedPumps = pumps.count { it.failed == true }
    val disabledPumps = snapshot.disabledPumpCount
    val activeRelays = snapshot.outputs.count { it == true }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Architectural 3-metric Station Summary Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(p.panel)
                    .border(1.1.dp, p.panelBorder, RoundedCornerShape(18.dp))
                    .padding(vertical = 13.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StationStatCell("Running", "$activePumps / ${pumps.size}", if (activePumps > 0) p.ok else p.textSecondary, Modifier.weight(1f))
                Box(modifier = Modifier.width(0.8.dp).height(34.dp).background(p.hairline))
                StationStatCell("Ready", "$enabledPumps / ${pumps.size}", p.cyan, Modifier.weight(1f))
                Box(modifier = Modifier.width(0.8.dp).height(34.dp).background(p.hairline))
                val healthLabel = when {
                    failedPumps > 0 -> "$failedPumps Fault"
                    disabledPumps > 0 -> "$disabledPumps Service (S)"
                    else -> "Normal"
                }
                val healthColor = when {
                    failedPumps > 0 -> p.fault
                    disabledPumps > 0 -> p.warning
                    else -> p.gold
                }
                StationStatCell("Health", healthLabel, healthColor, Modifier.weight(1f))
            }
        }

        item {
            ScadaPanel(
                title = "Pumps & Work Time",
                subtitle = "وضعیت کارکرد، حالت سرویس (S) و ساعات کارکرد هر پمپ",
                icon = Icons.Outlined.WaterDrop,
                accentColor = when {
                    failedPumps > 0 -> p.fault
                    disabledPumps > 0 -> p.warning
                    else -> p.ok
                },
            ) {
                pumps.forEachIndexed { idx, pump ->
                    if (idx > 0) HorizontalDivider(color = p.hairline)
                    val prFeedback = snapshot.prInputs.getOrNull(idx)
                    PumpDetailCardRow(pump = pump, prFeedback = prFeedback)
                }
            }
        }

        // Symmetrical 2-column × 5-row Industrial Relay Output Rack
        item {
            ScadaPanel(
                title = "Relay Outputs (OUT 0–9)",
                subtitle = "وضعیت لحظه‌ای رله‌های خروجی کنترلر",
                icon = Icons.Outlined.DeveloperBoard,
                accentColor = p.gold,
                trailing = { StatusBadge("$activeRelays ON", if (activeRelays > 0) BadgeTone.OK else BadgeTone.NEUTRAL) },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshot.outputs.chunked(2).forEachIndexed { rowIdx, pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            pair.forEachIndexed { colIdx, state ->
                                val outIndex = rowIdx * 2 + colIdx
                                PlcRackModuleCell(
                                    code = "OUT $outIndex",
                                    label = "Output $outIndex",
                                    active = state,
                                    activeColor = p.ok,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Symmetrical 2-column Digital Input Matrix (PR1–PR4 & MFI1–MFI4)
        item {
            ScadaPanel(
                title = "Input Status (PR & MFI)",
                subtitle = "وضعیت ورودی‌های فیدبک کنتاکتور و ورودی‌های چندمنظوره",
                icon = Icons.AutoMirrored.Outlined.Input,
                accentColor = p.cyan,
            ) {
                val inputs = buildList {
                    snapshot.prInputs.forEachIndexed { i, st ->
                        add(Triple("PR${i + 1}", "Pump ${i + 1} Feedback", st to p.cyan))
                    }
                    snapshot.mfiInputs.forEachIndexed { i, st ->
                        add(Triple("MFI${i + 1}", "Multi-Function Input ${i + 1}", st to p.gold))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    inputs.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            pair.forEach { (code, label, stateAndColor) ->
                                PlcRackModuleCell(
                                    code = code,
                                    label = label,
                                    active = stateAndColor.first,
                                    activeColor = stateAndColor.second,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            ScadaPanel(
                title = "Drive & Floater Status",
                subtitle = "وضعیت درایو (اینورتر) و الکترودهای سطح مخزن",
                icon = Icons.Outlined.Sensors,
                accentColor = p.cyan,
            ) {
                StatusLine(
                    label = "Drive Status",
                    value = snapshot.driveEnabled,
                    trueText = "Enable",
                    falseText = "Disable (S)",
                    trueTone = BadgeTone.OK,
                    falseTone = BadgeTone.WARNING,
                    onHelpClick = {
                        activeHelp = Bpc02Catalog.helpFor(com.dgp.boosterpump.controller.register.Bpc02RegisterMap.driveActivationDef) to
                            when (snapshot.driveEnabled) {
                                true -> "Enable"
                                false -> "Disable"
                                null -> null
                            }
                    },
                )
                HorizontalDivider(color = p.hairline)
                StatusLine("DR Status", snapshot.drStatus, "ON", "OFF", BadgeTone.OK, BadgeTone.NEUTRAL)
                HorizontalDivider(color = p.hairline)
                StatusLine("Drive Health", snapshot.driveFailed, "Fault", "Normal", BadgeTone.FAULT, BadgeTone.OK)
                HorizontalDivider(color = p.hairline)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val driverCount = snapshot.variablePumpCount
                    val freqLabel = when {
                        driverCount == 0 -> "Drives"
                        driverCount != null && driverCount >= 2 -> "Drive 1 / Drive 2 FRQ"
                        else -> "Drive FRQ"
                    }
                    val freqValue = when {
                        driverCount == 0 -> "0 Drives (DOL)"
                        driverCount != null && driverCount >= 2 -> {
                            val d1 = snapshot.drive1FrequencyHz?.let { "%.2f".format(it) } ?: "—"
                            val d2 = snapshot.drive2FrequencyHz?.let { "%.2f".format(it) } ?: "—"
                            "$d1 / $d2 Hz"
                        }
                        else -> snapshot.drive1FrequencyHz?.let { "%.2f Hz".format(it) } ?: "—"
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        LtrText(freqLabel, style = MaterialTheme.typography.bodyMedium, color = p.textPrimary)
                        ParameterHelpIcon(
                            onClick = { activeHelp = Bpc02Catalog.conceptHelp("drive_frequency") to freqValue },
                        )
                    }
                    LtrText(freqValue, style = MonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = p.cyan)
                }
                HorizontalDivider(color = p.hairline)
                StatusLine(
                    label = "Floater L Level",
                    value = snapshot.floaterLowConnected,
                    trueText = "Connected",
                    falseText = "Open (Low!)",
                    trueTone = BadgeTone.OK,
                    falseTone = BadgeTone.FAULT,
                    onHelpClick = {
                        activeHelp = Bpc02Catalog.conceptHelp("floater_level") to
                            if (snapshot.floaterLowConnected == true) "Connected" else "Open"
                    },
                )
                HorizontalDivider(color = p.hairline)
                StatusLine(
                    label = "Floater H Level",
                    value = snapshot.floaterHighConnected,
                    trueText = "Connected",
                    falseText = "Open",
                    trueTone = BadgeTone.OK,
                    falseTone = BadgeTone.WARNING,
                    onHelpClick = {
                        activeHelp = Bpc02Catalog.conceptHelp("floater_level") to
                            if (snapshot.floaterHighConnected == true) "Connected" else "Open"
                    },
                )
            }
        }
    }
}

@Composable
private fun StationStatCell(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    val p = LocalScadaPalette.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        LtrText(label, style = MaterialTheme.typography.labelSmall, color = p.textSecondary)
        LtrText(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = accent)
    }
}

@Composable
private fun PumpDetailCardRow(pump: PumpState, prFeedback: Boolean?) {
    val p = LocalScadaPalette.current
    val isDisabled = pump.enabled == false
    val isRunning = !isDisabled && pump.running == true
    val isReady = !isDisabled && pump.failed != true && !isRunning && pump.enabled == true
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedImpellerIcon(
            running = isRunning,
            failed = pump.failed,
            disabled = isDisabled,
            ready = isReady,
            size = 46.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LtrText("Pump ${pump.number}", style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
                if (prFeedback != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(p.panelElevated)
                            .border(0.7.dp, p.panelBorder, RoundedCornerShape(999.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        PlcLedBead(active = prFeedback, size = 6.dp, activeColor = p.cyan)
                        LtrText("PR${pump.number}", style = MonoStyle.copy(fontSize = 9.5.sp), color = p.textSecondary)
                    }
                }
            }
            val runTimeText = when {
                pump.runTime != null -> "Work Time: ${pump.runTime.format()}"
                pump.hasRunTimeRegisters -> "Work Time: —"
                else -> "No Work Time Register"
            }
            LtrText(runTimeText, style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
            if (isDisabled) {
                Text(
                    "خاموش‌شده در حالت سرویس (نمایش S روی کنترلر)",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.warning,
                )
            } else if (isReady) {
                Text(
                    "آماده ورود به مدار",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.cyan,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when {
                isDisabled -> StatusBadge("Service (S)", BadgeTone.WARNING)
                isRunning -> StatusBadge("Running", BadgeTone.OK, pulsing = true)
                isReady -> StatusBadge("Standby", BadgeTone.INFO)
                pump.running == false -> StatusBadge("Standby", BadgeTone.NEUTRAL)
                else -> StatusBadge("Unknown", BadgeTone.NEUTRAL)
            }
            if (pump.failed == true) {
                StatusBadge("Fault", BadgeTone.FAULT, pulsing = true)
            }
        }
    }
}

@Composable
private fun PlcRackModuleCell(
    code: String,
    label: String,
    active: Boolean?,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    val p = LocalScadaPalette.current
    val isActive = active == true
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) activeColor.copy(alpha = if (p.isDark) 0.11f else 0.09f) else p.panelElevated)
            .border(0.9.dp, if (isActive) activeColor.copy(alpha = 0.48f) else p.panelBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            LtrText(
                text = code,
                style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = if (isActive) activeColor else p.textPrimary,
            )
            LtrText(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
                color = p.textSecondary,
            )
        }
        PlcLedBead(active = active, size = 9.dp, activeColor = activeColor)
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
    onHelpClick: (() -> Unit)? = null,
) {
    val p = LocalScadaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LtrText(label, style = MaterialTheme.typography.bodyMedium, color = p.textPrimary)
            if (onHelpClick != null) {
                ParameterHelpIcon(onClick = onHelpClick)
            }
        }
        val (text, tone) = when (value) {
            true -> trueText to trueTone
            false -> falseText to falseTone
            null -> "Unknown" to BadgeTone.NEUTRAL
        }
        StatusBadge(text, tone, pulsing = tone == BadgeTone.FAULT)
    }
}
