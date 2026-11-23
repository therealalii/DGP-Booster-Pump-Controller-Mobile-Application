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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.controller.PumpState
import com.dgp.boosterpump.ui.PersianLabels
import com.dgp.boosterpump.ui.components.AnimatedImpellerIcon
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.LtrText
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
            Text("هنوز داده‌ای از کنترلر دریافت نشده است", style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text(
                "ابتدا از صفحه مانیتورینگ به مبدل سمین ES 487 یا حالت شبیه‌سازی متصل شوید.",
                style = MaterialTheme.typography.bodySmall,
                color = p.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val activePumps = snapshot.pumps.count { it.running == true }
    val failedPumps = snapshot.pumps.count { it.failed == true }
    val activeRelays = snapshot.outputs.count { it == true }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Architectural 3-metric Station Summary Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.panel)
                    .border(0.9.dp, p.panelBorder, RoundedCornerShape(16.dp))
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StationStatCell("پمپ در مدار", "$activePumps / 6", if (activePumps > 0) p.ok else p.textSecondary, Modifier.weight(1f))
                Box(modifier = Modifier.width(0.8.dp).height(34.dp).background(p.hairline))
                StationStatCell("رله خروجی فعال", "$activeRelays / 10", p.cyan, Modifier.weight(1f))
                Box(modifier = Modifier.width(0.8.dp).height(34.dp).background(p.hairline))
                StationStatCell(
                    "سلامت پمپ‌ها",
                    if (failedPumps > 0) "$failedPumps خطا" else "سالم",
                    if (failedPumps > 0) p.fault else p.gold,
                    Modifier.weight(1f),
                )
            }
        }

        item {
            ScadaPanel(
                title = "ایستگاه پمپاژ و ساعات کارکرد",
                subtitle = "کویل ۱۹–۳۱ (وضعیت و خطا) • ورد ۳۴–۴۵ (تایمر پمپ ۱–۴)",
                icon = Icons.Outlined.WaterDrop,
                accentColor = if (failedPumps > 0) p.fault else p.ok,
            ) {
                snapshot.pumps.forEachIndexed { idx, pump ->
                    if (idx > 0) HorizontalDivider(color = p.hairline)
                    val prFeedback = snapshot.prInputs.getOrNull(idx)
                    PumpDetailCardRow(pump = pump, prFeedback = prFeedback)
                }
            }
        }

        // Symmetrical 2-column × 5-row Industrial PLC Relay Rack (OUT 0–9)
        item {
            ScadaPanel(
                title = "بانک رله‌های خروجی دیجیتال (OUT 0–9)",
                subtitle = "وضعیت لحظه‌ای ۱۰ رله خروجی کنترلر — کویل ۰ تا ۹ (FC01)",
                icon = Icons.Outlined.DeveloperBoard,
                accentColor = p.gold,
                trailing = { StatusBadge("$activeRelays روشن", if (activeRelays > 0) BadgeTone.OK else BadgeTone.NEUTRAL) },
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
                                    label = "رله خروجی $outIndex",
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

        // Symmetrical 2-column PLC Digital Input Matrix (PR1–PR4 & MFI1–MFI4)
        item {
            ScadaPanel(
                title = "ورودی‌های برگشت فرمان و چندمنظوره",
                subtitle = "کنتاکتور PR1–PR4 (کویل ۱۱–۱۴) • ورودی‌های MFI1–MFI4 (کویل ۱۵–۱۸)",
                icon = Icons.AutoMirrored.Outlined.Input,
                accentColor = p.cyan,
            ) {
                val inputs = buildList {
                    snapshot.prInputs.forEachIndexed { i, st ->
                        add(Triple("PR${i + 1}", "فیدبک پمپ ${i + 1}", st to p.cyan))
                    }
                    snapshot.mfiInputs.forEachIndexed { i, st ->
                        add(Triple("MFI${i + 1}", "ورودی چندمنظوره ${i + 1}", st to p.gold))
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
                title = "سنسورهای درایو و فلوتر مخزن",
                subtitle = "کویل ۱۰ (DR) • کویل ۲۵ (خطای درایو) • کویل ۳۲–۳۳ (فلوتر L / H)",
                icon = Icons.Outlined.Sensors,
                accentColor = p.cyan,
            ) {
                StatusLine("وضعیت DR (کویل ۱۰)", snapshot.drStatus, "فعال", "خاموش", BadgeTone.OK, BadgeTone.NEUTRAL)
                HorizontalDivider(color = p.hairline)
                StatusLine("خطای درایو (کویل ۲۵)", snapshot.driveFailed, "دارای خطا", "عادی", BadgeTone.FAULT, BadgeTone.OK)
                HorizontalDivider(color = p.hairline)
                StatusLine("فلوتر سطح پایین مخزن — L (کویل ۳۲)", snapshot.floaterLowConnected, "متصل", "قطع (کم‌آبی)", BadgeTone.OK, BadgeTone.FAULT)
                HorizontalDivider(color = p.hairline)
                StatusLine("فلوتر سطح بالا مخزن — H (کویل ۳۳)", snapshot.floaterHighConnected, "متصل", "قطع", BadgeTone.OK, BadgeTone.WARNING)
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
        Text(label, style = MaterialTheme.typography.labelSmall, color = p.textSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = accent)
    }
}

@Composable
private fun PumpDetailCardRow(pump: PumpState, prFeedback: Boolean?) {
    val p = LocalScadaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedImpellerIcon(running = pump.running, failed = pump.failed, size = 44.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("پمپ ${pump.number}", style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
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
                pump.runTime != null -> "زمان کارکرد: ${PersianLabels.formatRunTime(pump.runTime)}"
                pump.hasRunTimeRegisters -> "زمان کارکرد: در دسترس نیست"
                else -> "تایمر کارکرد برای پمپ ${pump.number} تعریف نشده است"
            }
            Text(runTimeText, style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val (runLabel, runTone) = when (pump.running) {
                true -> "در حال کار" to BadgeTone.OK
                false -> "توقف" to BadgeTone.NEUTRAL
                null -> "نامشخص" to BadgeTone.NEUTRAL
            }
            StatusBadge(runLabel, runTone, pulsing = pump.running == true)
            if (pump.failed == true) {
                StatusBadge("خطای پمپ", BadgeTone.FAULT, pulsing = true)
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
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = p.textSecondary, maxLines = 1)
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
) {
    val p = LocalScadaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = p.textPrimary, modifier = Modifier.weight(1f))
        val (text, tone) = when (value) {
            true -> trueText to trueTone
            false -> falseText to falseTone
            null -> "نامشخص" to BadgeTone.NEUTRAL
        }
        StatusBadge(text, tone, pulsing = tone == BadgeTone.FAULT)
    }
}
