package com.dgp.boosterpump.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Input
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PumpsAndIoScreen(uiState: MonitoringUiState) {
    val p = LocalScadaPalette.current
    val snapshot = uiState.snapshot
    if (snapshot == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("هنوز داده‌ای از کنترلر دریافت نشده است", style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
            Text(
                "ابتدا از صفحه داشبورد به مبدل سمین ES 487 یا حالت شبیه‌سازی متصل شوید.",
                style = MaterialTheme.typography.bodySmall,
                color = p.textSecondary,
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
            ScadaPanel(
                title = "ایستگاه پمپاژ و ساعات کارکرد",
                subtitle = "وضعیت پمپ‌های ۱–۶ (کویل ۱۹–۳۱) • تایمر کارکرد پمپ‌های ۱–۴ (ورد ۳۴–۴۵ تایید سخت‌افزاری)",
                icon = Icons.Outlined.WaterDrop,
                accentColor = p.ok,
            ) {
                snapshot.pumps.forEachIndexed { idx, pump ->
                    if (idx > 0) HorizontalDivider(color = p.panelBorder)
                    val prFeedback = snapshot.prInputs.getOrNull(idx)
                    PumpDetailCardRow(pump = pump, prFeedback = prFeedback)
                }
            }
        }

        item {
            ScadaPanel(
                title = "بانک رله‌های خروجی دیجیتال (OUT 0–9)",
                subtitle = "وضعیت لحظه‌ای ۱۰ رله خروجی کنترلر — کویل‌های ۰ تا ۹ (FC01)",
                icon = Icons.Outlined.DeveloperBoard,
                accentColor = p.cyan,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    snapshot.outputs.forEachIndexed { index, state ->
                        PlcIoModuleChip(
                            code = "OUT $index",
                            label = "رله $index",
                            active = state,
                            activeColor = p.ok,
                        )
                    }
                }
            }
        }

        item {
            ScadaPanel(
                title = "ورودی‌های برگشت فرمان و چندمنظوره",
                subtitle = "کنتاکتور مستقیم PR1–PR4 (کویل ۱۱–۱۴) • ورودی‌های قابل تعریف MFI1–MFI4 (کویل ۱۵–۱۸)",
                icon = Icons.AutoMirrored.Outlined.Input,
                accentColor = p.cyan,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    snapshot.prInputs.forEachIndexed { index, state ->
                        PlcIoModuleChip(
                            code = "PR${index + 1}",
                            label = "فیدبک پمپ ${index + 1}",
                            active = state,
                            activeColor = p.cyan,
                        )
                    }
                    snapshot.mfiInputs.forEachIndexed { index, state ->
                        PlcIoModuleChip(
                            code = "MFI${index + 1}",
                            label = "ورودی ${index + 1}",
                            active = state,
                            activeColor = p.warning,
                        )
                    }
                }
            }
        }

        item {
            ScadaPanel(
                title = "سنسورهای درایو و فلوتر مخزن",
                subtitle = "کویل ۱۰ (DR) • کویل ۲۵ (خطای درایو) • کویل ۳۲–۳۳ (فلوترهای L و H)",
                icon = Icons.Outlined.Sensors,
                accentColor = p.cyan,
            ) {
                StatusLine("وضعیت DR (کویل ۱۰)", snapshot.drStatus, "فعال", "خاموش", BadgeTone.OK, BadgeTone.NEUTRAL)
                HorizontalDivider(color = p.panelBorder)
                StatusLine("خطای درایو (کویل ۲۵)", snapshot.driveFailed, "دارای خطا", "عادی", BadgeTone.FAULT, BadgeTone.OK)
                HorizontalDivider(color = p.panelBorder)
                StatusLine("فلوتر سطح پایین مخزن — L (کویل ۳۲)", snapshot.floaterLowConnected, "متصل", "قطع (کم‌آبی)", BadgeTone.OK, BadgeTone.FAULT)
                HorizontalDivider(color = p.panelBorder)
                StatusLine("فلوتر سطح بالا مخزن — H (کویل ۳۳)", snapshot.floaterHighConnected, "متصل", "قطع", BadgeTone.OK, BadgeTone.WARNING)
            }
        }
    }
}

@Composable
private fun PumpDetailCardRow(pump: PumpState, prFeedback: Boolean?) {
    val p = LocalScadaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedImpellerIcon(running = pump.running, failed = pump.failed, size = 44.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("پمپ ${pump.number}", style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
                if (prFeedback != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        PlcLedBead(active = prFeedback, size = 7.dp, activeColor = p.cyan)
                        LtrText("PR${pump.number}", style = MonoStyle.copy(fontSize = 10.sp), color = p.textSecondary)
                    }
                }
            }
            val runTimeText = when {
                pump.runTime != null -> "زمان کارکرد: ${PersianLabels.formatRunTime(pump.runTime)}"
                pump.hasRunTimeRegisters -> "زمان کارکرد: در دسترس نیست"
                else -> "تایمر برای پمپ ${pump.number} در دفترچه BPC-02 تعریف نشده است"
            }
            Text(runTimeText, style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val (runLabel, runTone) = when (pump.running) {
                true -> "در حال کار" to BadgeTone.OK
                false -> "توقف" to BadgeTone.NEUTRAL
                null -> "نامشخص" to BadgeTone.NEUTRAL
            }
            StatusBadge(runLabel, runTone)
            if (pump.failed == true) {
                StatusBadge("خطای پمپ", BadgeTone.FAULT)
            }
        }
    }
}

@Composable
private fun PlcIoModuleChip(
    code: String,
    label: String,
    active: Boolean?,
    activeColor: androidx.compose.ui.graphics.Color,
) {
    val p = LocalScadaPalette.current
    val border = if (active == true) activeColor.copy(alpha = 0.65f) else p.panelBorder
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active == true) activeColor.copy(alpha = 0.12f) else p.panelElevated)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        PlcLedBead(active = active, size = 9.dp, activeColor = activeColor)
        Column {
            LtrText(
                text = code,
                style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = if (active == true) activeColor else p.textPrimary,
            )
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = p.textSecondary)
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
    val p = LocalScadaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = p.textPrimary, modifier = Modifier.weight(1f))
        val (text, tone) = when (value) {
            true -> trueText to trueTone
            false -> falseText to falseTone
            null -> "نامشخص" to BadgeTone.NEUTRAL
        }
        StatusBadge(text, tone)
    }
}
