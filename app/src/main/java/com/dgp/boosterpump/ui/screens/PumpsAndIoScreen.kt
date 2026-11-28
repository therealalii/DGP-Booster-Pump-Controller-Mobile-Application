package com.dgp.boosterpump.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.controller.Bpc02Catalog
import com.dgp.boosterpump.controller.PumpState
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PumpsAndIoScreen(uiState: MonitoringUiState) {
    val p = LocalScadaPalette.current
    val snapshot = uiState.snapshot
    val cfgReadingsByKey = remember(uiState.configSnapshot.readings) {
        uiState.configSnapshot.readings.associate { it.definition.key to it.raw }
    }
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

    // Resolve live MFI1..4 and MFO1..4 configured types (live slow-poll with config snapshot fallback)
    val resolvedMfiTypes = (0 until Bpc02RegisterMap.MFI_INPUT_COUNT).map { idx ->
        snapshot.mfiTypes.getOrNull(idx) ?: cfgReadingsByKey["mfi_${idx + 1}_type"]
    }
    val resolvedMfoTypes = (0 until Bpc02RegisterMap.MFO_OUTPUT_COUNT).map { idx ->
        snapshot.mfoTypes.getOrNull(idx) ?: cfgReadingsByKey["mfo_${idx + 1}_type"]
    }
    val activeMfiOptionNames = resolvedMfiTypes.mapNotNull { raw ->
        raw?.takeIf { it != 0 }?.let { Bpc02Catalog.mfiOptionLabel(it) }
    }.toSet()
    val activeMfoOptionNames = resolvedMfoTypes.mapNotNull { raw ->
        raw?.takeIf { it != 0 }?.let { Bpc02Catalog.mfoOptionLabel(it) }
    }.toSet()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // 1. Architectural 3-metric Station Summary Header
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

        // 2. Pumps & Work Time
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

        // 3. Input Status FIRST (Drive Input [Coil 10], PR1–PR4, MFI1–MFI4 + Live Assigned Option & Allowed Options)
        item {
            val activeInputs = listOfNotNull(snapshot.drStatus).count { it } +
                snapshot.prInputs.count { it == true } +
                snapshot.mfiInputs.count { it == true }
            ScadaPanel(
                title = "Input Status (Drive, PR & MFI)",
                subtitle = "وضعیت ورودی دیجیتال درایو، فیدبک کنتاکتورها (PR1–PR4) و ورودی‌های چندمنظوره (MFI1–MFI4)",
                icon = Icons.AutoMirrored.Outlined.Input,
                accentColor = p.cyan,
                trailing = { StatusBadge("$activeInputs ON", if (activeInputs > 0) BadgeTone.INFO else BadgeTone.NEUTRAL) },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Drive Hardware Feedback Input (Coil 10 — matches physical controller Inputs LCD `ODrive`)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PlcRackModuleCell(
                            code = "Drive (DR)",
                            label = "Drive Feedback Input",
                            assignedOption = null,
                            active = snapshot.drStatus,
                            activeColor = p.ok,
                            modifier = Modifier.weight(1f),
                        )
                        ParameterHelpIcon(
                            onClick = {
                                val cur = when (snapshot.drStatus) {
                                    true -> "ON"
                                    false -> "OFF"
                                    null -> null
                                }
                                activeHelp = Bpc02Catalog.conceptHelp("dr_status") to cur
                            },
                        )
                    }

                    // PR1–PR4 Feedback Matrix (2x2)
                    val prList = snapshot.prInputs.mapIndexed { i, st ->
                        Triple("PR${i + 1}", "Pump ${i + 1} Feedback", st)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        prList.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                pair.forEach { (code, label, st) ->
                                    PlcRackModuleCell(
                                        code = code,
                                        label = label,
                                        assignedOption = null,
                                        active = st,
                                        activeColor = p.cyan,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    HorizontalDivider(color = p.hairline)

                    // MFI1–MFI4 Matrix with Live Configured Assignment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            LtrText(
                                text = "MFI1–MFI4 (Multi-Function Inputs)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = p.gold,
                            )
                            ParameterHelpIcon(
                                onClick = { activeHelp = Bpc02Catalog.conceptHelp("mfi_overview") to null },
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        snapshot.mfiInputs.chunked(2).forEachIndexed { rowIdx, pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                pair.forEachIndexed { colIdx, state ->
                                    val mfiIndex = rowIdx * 2 + colIdx
                                    val mfiNum = mfiIndex + 1
                                    val assigned = Bpc02Catalog.mfiOptionLabel(resolvedMfiTypes.getOrNull(mfiIndex))
                                    PlcRackModuleCell(
                                        code = "MFI$mfiNum",
                                        label = "Input $mfiNum",
                                        assignedOption = assigned,
                                        active = state,
                                        activeColor = p.gold,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    // Allowed MFI Options Read-Only Reference Box
                    AllowedOptionsReferenceBox(
                        title = "MFI Allowed Options (Read-Only)",
                        options = Bpc02Catalog.mfiAllowedOptions,
                        activeOptions = activeMfiOptionNames,
                        accentColor = p.gold,
                        noteFa = "ورودی‌های MFI1–MFI4 روی یکی از گزینه‌های بالا تنظیم می‌شوند. نوع عملکرد آن‌ها در ModBus فقط‌خواندنی است و فقط از منوی کنترلر (Hardware › 5. MFx) قابل تغییر است.",
                    )
                }
            }
        }

        // 4. Relay Outputs AFTER Input Status (1-based OUT 1–10; last 4 outputs OUT 7–10 = MFO1–MFO4)
        item {
            ScadaPanel(
                title = "Relay Outputs (OUT 1–10)",
                subtitle = "وضعیت لحظه‌ای رله‌های خروجی کنترلر (خروجی‌های ۱ تا ۶ و رله‌های چندمنظوره ۷ تا ۱۰)",
                icon = Icons.Outlined.DeveloperBoard,
                accentColor = p.gold,
                trailing = { StatusBadge("$activeRelays ON", if (activeRelays > 0) BadgeTone.OK else BadgeTone.NEUTRAL) },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // OUT 1..OUT 6 (Coils 0..5)
                    val primaryOutputs = snapshot.outputs.take(6)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        primaryOutputs.chunked(2).forEachIndexed { rowIdx, pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                pair.forEachIndexed { colIdx, state ->
                                    val outNum = rowIdx * 2 + colIdx + 1
                                    PlcRackModuleCell(
                                        code = "OUT $outNum",
                                        label = "Output $outNum",
                                        assignedOption = null,
                                        active = state,
                                        activeColor = p.ok,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    HorizontalDivider(color = p.hairline)

                    // Last 4 Outputs: OUT 7..OUT 10 = MFO1..MFO4 (Coils 6..9, Words 4184..4187)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        LtrText(
                            text = "OUT 7–10 (MFO1–MFO4 Multi-Function)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = p.cyan,
                        )
                        ParameterHelpIcon(
                            onClick = { activeHelp = Bpc02Catalog.conceptHelp("mfo_overview") to null },
                        )
                    }

                    val mfoOutputs = snapshot.outputs.drop(6)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        mfoOutputs.chunked(2).forEachIndexed { rowIdx, pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                pair.forEachIndexed { colIdx, state ->
                                    val mfoIndex = rowIdx * 2 + colIdx
                                    val outNum = 7 + mfoIndex
                                    val mfoNum = mfoIndex + 1
                                    val assigned = Bpc02Catalog.mfoOptionLabel(resolvedMfoTypes.getOrNull(mfoIndex))
                                    PlcRackModuleCell(
                                        code = "OUT $outNum",
                                        label = "MFO$mfoNum",
                                        assignedOption = assigned,
                                        active = state,
                                        activeColor = p.ok,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    // Allowed MFO Options Read-Only Reference Box
                    AllowedOptionsReferenceBox(
                        title = "MFO1–MFO4 Allowed Options (Read-Only)",
                        options = Bpc02Catalog.mfoAllowedOptions,
                        activeOptions = activeMfoOptionNames,
                        accentColor = p.cyan,
                        noteFa = "چهار خروجی آخر (OUT 7 تا OUT 10) رله‌های چندمنظوره MFO1–MFO4 هستند. عملکرد آن‌ها از طریق ModBus فقط‌خواندنی است و فقط از منوی کنترلر (Hardware › 5. MFx) ویرایش می‌شود.",
                    )
                }
            }
        }

        // 5. Drive & Floater Status (Clean — no redundant DR Status row; clearer Floater H Level label & calm blue tone)
        item {
            ScadaPanel(
                title = "Drive & Floater Status",
                subtitle = "وضعیت سرویس درایو (اینورتر)، سلامت و الکترودهای سطح مخزن",
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
                        activeHelp = Bpc02Catalog.helpFor(Bpc02RegisterMap.driveActivationDef) to
                            when (snapshot.driveEnabled) {
                                true -> "Enable"
                                false -> "Disable"
                                null -> null
                            }
                    },
                )
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
                    trueText = "Water OK",
                    falseText = "Low Water!",
                    trueTone = BadgeTone.OK,
                    falseTone = BadgeTone.FAULT,
                    onHelpClick = {
                        activeHelp = Bpc02Catalog.conceptHelp("floater_level") to
                            if (snapshot.floaterLowConnected == true) "Water OK" else "Low Water!"
                    },
                )
                HorizontalDivider(color = p.hairline)
                StatusLine(
                    label = "Floater H Level",
                    value = snapshot.floaterHighConnected,
                    trueText = "Full (High)",
                    falseText = "Below High",
                    trueTone = BadgeTone.OK,
                    falseTone = BadgeTone.INFO,
                    onHelpClick = {
                        activeHelp = Bpc02Catalog.conceptHelp("floater_level") to
                            if (snapshot.floaterHighConnected == true) "Full (High)" else "Below High"
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllowedOptionsReferenceBox(
    title: String,
    options: List<String>,
    activeOptions: Set<String>,
    accentColor: Color,
    noteFa: String,
) {
    val p = LocalScadaPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.panelElevated)
            .border(0.85.dp, p.panelBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        LtrText(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = accentColor,
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                options.forEach { opt ->
                    val isAssigned = opt in activeOptions
                    Surface(
                        color = if (isAssigned) accentColor.copy(alpha = 0.16f) else p.panel,
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(
                            width = 0.75.dp,
                            color = if (isAssigned) accentColor.copy(alpha = 0.65f) else p.panelBorder,
                        ),
                    ) {
                        Text(
                            text = opt,
                            style = MonoStyle.copy(
                                fontSize = 9.5.sp,
                                fontWeight = if (isAssigned) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (isAssigned) accentColor else p.textSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
        Text(
            text = noteFa,
            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
            color = p.textSecondary,
        )
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
    assignedOption: String?,
    active: Boolean?,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    val p = LocalScadaPalette.current
    val isActive = active == true
    val hasConfiguredFunc = assignedOption != null && assignedOption != "—" && assignedOption != "Disable"
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) activeColor.copy(alpha = if (p.isDark) 0.11f else 0.09f) else p.panelElevated)
            .border(0.9.dp, if (isActive) activeColor.copy(alpha = 0.48f) else p.panelBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
        if (assignedOption != null) {
            Surface(
                color = if (hasConfiguredFunc) activeColor.copy(alpha = 0.14f) else p.panel,
                shape = RoundedCornerShape(7.dp),
                border = BorderStroke(
                    0.7.dp,
                    if (hasConfiguredFunc) activeColor.copy(alpha = 0.45f) else p.panelBorder,
                ),
            ) {
                LtrText(
                    text = "Set: $assignedOption",
                    style = MonoStyle.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                    color = if (hasConfiguredFunc) activeColor else p.textMuted,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
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
