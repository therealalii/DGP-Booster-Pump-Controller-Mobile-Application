package com.dgp.boosterpump.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.dgp.boosterpump.controller.AlarmSeverity
import com.dgp.boosterpump.controller.ControllerSnapshot
import com.dgp.boosterpump.controller.PumpState
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.ui.PersianLabels
import com.dgp.boosterpump.ui.components.AnimatedImpellerIcon
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.BoosterHydraulicFlowDiagram
import com.dgp.boosterpump.ui.components.DgpGoldButton
import com.dgp.boosterpump.ui.components.LtrText
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.PressureArcGauge
import com.dgp.boosterpump.ui.components.PressureTrendChart
import com.dgp.boosterpump.ui.components.ScadaPanel
import com.dgp.boosterpump.ui.components.StatusBadge
import com.dgp.boosterpump.ui.components.ThreePhaseVoltageBars
import com.dgp.boosterpump.ui.components.WaterTankIndicator
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.ui.theme.MonoStyle
import com.dgp.boosterpump.viewmodel.MonitoringUiState

@Composable
fun DashboardScreen(
    uiState: MonitoringUiState,
    canResetErrors: Boolean,
    canOpenGatewaySettings: Boolean,
    onConnect: () -> Unit,
    onResetPumpErrors: () -> Unit,
    onOpenGatewaySettings: () -> Unit,
    onOpenPumps: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val snapshot = uiState.snapshot
    val conn = uiState.connectionState
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = p.panel,
            shape = RoundedCornerShape(22.dp),
            icon = { Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = p.gold) },
            title = { Text("تایید رفع خطای پمپ‌ها", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "فرمان رفع خطای پمپ‌ها به کنترلر ارسال خواهد شد.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.textPrimary,
                    )
                    Text(
                        "پیش از تایید، از رفع عامل خطا و ایمنی تجهیزات اطمینان حاصل کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.warning,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetPumpErrors()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD09629), contentColor = Color(0xFF0A0702)),
                ) {
                    Text("تایید و رفع خطا", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("انصراف", color = p.textSecondary) }
            },
        )
    }

    if (snapshot == null) {
        EmptyConnectionState(
            uiState = uiState,
            canOpenGatewaySettings = canOpenGatewaySettings,
            onConnect = onConnect,
            onOpenGatewaySettings = onOpenGatewaySettings,
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (conn is ConnectionState.Error) {
            item {
                NoticeBox(
                    text = "ارتباط با مبدل شبکه برقرار نیست — در حال تلاش مجدد برای اتصال…",
                    tone = BadgeTone.FAULT,
                )
            }
        } else if (conn is ConnectionState.Connected && conn.partialError != null) {
            item {
                NoticeBox(
                    text = "بخشی از اطلاعات دستگاه با تاخیر دریافت شد؛ در چرخه بعدی بروزرسانی می‌شود.",
                    tone = BadgeTone.WARNING,
                )
            }
        }

        item { HeroTelemetryPanel(snapshot) }

        item {
            ScadaPanel(
                title = "شماتیک هیدرولیک ایستگاه",
                subtitle = "نمای زنده مخزن، پمپاژ و فشار خط خروجی",
                icon = Icons.Outlined.Hub,
                accentColor = if (snapshot.alarms.isNotEmpty()) p.fault else p.gold,
            ) {
                BoosterHydraulicFlowDiagram(
                    pressureBar = snapshot.pressureBar,
                    runningPumpCount = snapshot.runningPumpCount,
                    floaterLow = snapshot.floaterLowConnected,
                    hasFault = snapshot.alarms.isNotEmpty(),
                )
            }
        }

        item {
            AlarmsPanel(
                snapshot = snapshot,
                canReset = canResetErrors && conn is ConnectionState.Connected && uiState.configSnapshot.writingKey == null,
                onRequestReset = { showResetDialog = true },
            )
        }

        item {
            ScadaPanel(
                title = "نمودار تغییرات فشار",
                subtitle = "روند فشار سیستم در لحظات اخیر",
                icon = Icons.AutoMirrored.Outlined.ShowChart,
                accentColor = p.cyan,
            ) {
                PressureTrendChart(samples = uiState.pressureHistory)
            }
        }

        item { BoosterStationPumpsPanel(snapshot.visiblePumps, onOpenPumps) }

        item { ElectricalAnalyzerPanel(snapshot) }
    }
}

@Composable
private fun HeroTelemetryPanel(snapshot: ControllerSnapshot) {
    val p = LocalScadaPalette.current
    val modeLabel = PersianLabels.controlStatusShort(snapshot.controlStatus)
    val modeTone = when (snapshot.controlStatus) {
        1, 2 -> BadgeTone.GOLD
        0 -> BadgeTone.WARNING
        else -> BadgeTone.NEUTRAL
    }
    val visiblePumps = snapshot.visiblePumps

    ScadaPanel(
        title = "وضعیت اصلی بوستر پمپ",
        subtitle = "پایش زنده فشار، مخزن و مد کاری ایستگاه",
        icon = Icons.Outlined.Speed,
        accentColor = p.gold,
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(modeLabel, modeTone)
                val (label, tone) = when {
                    snapshot.driveFailed == true -> "خطای درایو" to BadgeTone.FAULT
                    snapshot.driveEnabled == false -> "درایو خاموش" to BadgeTone.WARNING
                    snapshot.driveFailed == false -> "درایو سالم" to BadgeTone.OK
                    else -> "درایو —" to BadgeTone.NEUTRAL
                }
                StatusBadge(label, tone, pulsing = snapshot.driveFailed == true)
            }
        },
    ) {
        // Prominent Operating Mode + Connected/Configured Drivers banner (at-a-glance on Home screen)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(p.panelElevated)
                .border(0.8.dp, p.gold.copy(alpha = 0.28f), RoundedCornerShape(13.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("مد کاری دستگاه", style = MaterialTheme.typography.labelSmall, color = p.textSecondary)
                Text(
                    text = PersianLabels.wiringModeLabel(snapshot.wiringMode),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = p.gold,
                    maxLines = 1,
                )
            }
            Box(
                modifier = Modifier
                    .width(0.8.dp)
                    .height(30.dp)
                    .background(p.hairline),
            )
            Column(
                modifier = Modifier
                    .weight(0.75f)
                    .padding(start = 10.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("تعداد درایو", style = MaterialTheme.typography.labelSmall, color = p.textSecondary)
                Text(
                    text = PersianLabels.driverCountLabel(snapshot.variablePumpCount),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = p.cyan,
                    maxLines = 1,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PressureArcGauge(
                pressureBar = snapshot.pressureBar,
                setpointBar = snapshot.pressureSetpointBar,
                drActive = snapshot.drStatus,
            )
            WaterTankIndicator(
                floaterLow = snapshot.floaterLowConnected,
                floaterHigh = snapshot.floaterHighConnected,
            )
        }

        // Unified architectural 3-column KPI rail with crisp vertical hairline separators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(p.panelElevated)
                .border(0.8.dp, p.panelBorder, RoundedCornerShape(14.dp))
                .padding(vertical = 11.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val driverCount = snapshot.variablePumpCount
            val (driveTitle, driveValue, driveCaption) = when {
                driverCount == 0 -> Triple("وضعیت درایو", "بدون درایو", "کارکرد دورثابت")
                driverCount != null && driverCount >= 2 -> Triple(
                    "فرکانس درایو ۱",
                    snapshot.drive1FrequencyHz?.let { "%.2f Hz".format(it) } ?: "—",
                    snapshot.drive2FrequencyHz?.let { "درایو ۲: %.2f Hz".format(it) } ?: "۲ درایو متصل",
                )
                else -> Triple(
                    "فرکانس درایو",
                    snapshot.drive1FrequencyHz?.let { "%.2f Hz".format(it) } ?: "—",
                    when {
                        snapshot.driveEnabled == false -> "درایو غیرفعال (S)"
                        snapshot.drStatus == true -> "درایو در مدار"
                        else -> "آماده‌به‌کار"
                    },
                )
            }
            KpiRailCell(
                title = driveTitle,
                value = driveValue,
                caption = driveCaption,
                accent = p.warning,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .width(0.8.dp)
                    .height(40.dp)
                    .background(p.hairline),
            )
            KpiRailCell(
                title = "فرکانس برق شهر",
                value = snapshot.mainsFrequencyHz?.let { "%.2f Hz".format(it) } ?: "—",
                caption = "برق ورودی",
                accent = p.cyan,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .width(0.8.dp)
                    .height(40.dp)
                    .background(p.hairline),
            )
            val enabledCount = snapshot.enabledPumpCount
            val pumpCaption = when {
                snapshot.disabledPumpCount > 0 -> "$enabledCount آماده • ${snapshot.disabledPumpCount} خاموش (S)"
                enabledCount > 0 -> "$enabledCount پمپ آماده"
                else -> "مجموع ${visiblePumps.size} پمپ"
            }
            KpiRailCell(
                title = "پمپ در حال کار",
                value = "${snapshot.runningPumpCount} / ${visiblePumps.size}",
                caption = pumpCaption,
                accent = when {
                    snapshot.disabledPumpCount > 0 && snapshot.runningPumpCount == 0 -> p.warning
                    snapshot.runningPumpCount > 0 -> p.ok
                    else -> p.gold
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KpiRailCell(
    title: String,
    value: String,
    caption: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val p = LocalScadaPalette.current
    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = p.textSecondary, maxLines = 1)
        LtrText(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = accent,
        )
        Text(caption, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = p.textMuted, maxLines = 1)
    }
}

@Composable
private fun AlarmsPanel(
    snapshot: ControllerSnapshot,
    canReset: Boolean,
    onRequestReset: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val alarms = snapshot.alarms
    val hasAlarms = alarms.isNotEmpty()
    ScadaPanel(
        title = "وضعیت حفاظت و خطاها",
        subtitle = "پایش خودکار خطاهای پمپ، درایو، فلوتر و برق ورودی",
        icon = if (hasAlarms) Icons.Outlined.NotificationsActive else Icons.Outlined.CheckCircleOutline,
        accentColor = if (hasAlarms) p.fault else p.ok,
        borderColor = if (hasAlarms) p.fault.copy(alpha = 0.55f) else null,
        trailing = {
            if (hasAlarms) {
                StatusBadge("${alarms.size} خطای فعال", BadgeTone.FAULT, pulsing = true)
            } else {
                StatusBadge("بدون خطا", BadgeTone.OK)
            }
        },
    ) {
        if (!hasAlarms) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(p.okBg)
                    .border(0.8.dp, p.ok.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null, tint = p.ok, modifier = Modifier.size(19.dp))
                Text(
                    "تمام حفاظت‌های ایستگاه، کنترل فاز و درایو در وضعیت عادی هستند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.ok,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                alarms.forEach { alarm ->
                    val (title, detail) = PersianLabels.localizeAlarm(alarm)
                    NoticeBox(
                        text = "$title — $detail",
                        tone = if (alarm.severity == AlarmSeverity.FAULT) BadgeTone.FAULT else BadgeTone.WARNING,
                    )
                }
                if (canReset) {
                    Spacer(Modifier.height(2.dp))
                    DgpGoldButton(
                        text = "رفع خطای پمپ‌ها",
                        icon = Icons.Outlined.RestartAlt,
                        onClick = onRequestReset,
                        height = 44.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoosterStationPumpsPanel(pumps: List<PumpState>, onOpenPumps: () -> Unit) {
    val p = LocalScadaPalette.current
    val running = pumps.count { it.running == true && it.enabled != false }
    val enabled = pumps.count { it.enabled == true }
    val failed = pumps.count { it.failed == true }
    val disabled = pumps.count { it.enabled == false }
    val subtitle = buildString {
        if (failed > 0) append("$failed خطا")
        if (enabled > 0) {
            if (isNotEmpty()) append(" • ")
            append("$enabled آماده در مدار")
        }
        if (disabled > 0) {
            if (isNotEmpty()) append(" • ")
            append("$disabled خاموش (S)")
        }
        if (isEmpty()) append("وضعیت لحظه‌ای پمپ‌های ایستگاه")
    }
    ScadaPanel(
        title = "وضعیت پمپ‌ها ($running از ${pumps.size} در حال کار)",
        subtitle = subtitle,
        icon = Icons.Outlined.WaterDrop,
        accentColor = when {
            failed > 0 -> p.fault
            disabled > 0 -> p.warning
            else -> p.ok
        },
        trailing = {
            Surface(
                onClick = onOpenPumps,
                color = p.panelElevated,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(0.8.dp, p.panelBorder),
            ) {
                Text(
                    "جزئیات بیشتر",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = p.gold,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                )
            }
        },
    ) {
        pumps.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { pump ->
                    PumpHmiTile(pump = pump, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PumpHmiTile(pump: PumpState, modifier: Modifier = Modifier) {
    val p = LocalScadaPalette.current
    val isDisabled = pump.enabled == false
    val isRunning = !isDisabled && pump.running == true
    val isReady = !isDisabled && pump.failed != true && !isRunning && pump.enabled == true
    val borderColor = when {
        pump.failed == true -> p.fault.copy(alpha = 0.55f)
        isDisabled -> p.warning.copy(alpha = 0.58f)
        isRunning -> p.ok.copy(alpha = 0.48f)
        isReady -> p.cyan.copy(alpha = 0.34f)
        else -> p.panelBorder
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDisabled) p.warningBg.copy(alpha = 0.45f) else p.panelElevated)
            .border(0.9.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedImpellerIcon(
            running = isRunning,
            failed = pump.failed,
            disabled = isDisabled,
            ready = isReady,
            size = 42.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("پمپ ${pump.number}", style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
            val (label, tone) = when {
                pump.failed == true -> "خطا" to BadgeTone.FAULT
                isDisabled -> "خاموش (S)" to BadgeTone.WARNING
                isRunning -> "در حال کار" to BadgeTone.OK
                isReady -> "آماده‌به‌کار" to BadgeTone.INFO
                pump.running == false -> "آماده‌به‌کار" to BadgeTone.NEUTRAL
                else -> "نامشخص" to BadgeTone.NEUTRAL
            }
            StatusBadge(label, tone, pulsing = pump.failed == true || isRunning)
            LtrText(
                text = when {
                    pump.runTime != null -> PersianLabels.formatRunTimeCompact(pump.runTime)
                    pump.hasRunTimeRegisters -> "--:--:--"
                    else -> "—"
                },
                style = MonoStyle.copy(fontSize = 10.5.sp),
                color = p.textSecondary,
            )
        }
    }
}

@Composable
private fun ElectricalAnalyzerPanel(snapshot: ControllerSnapshot) {
    val p = LocalScadaPalette.current
    ScadaPanel(
        title = "وضعیت برق ورودی و ولتاژ فازها",
        subtitle = "ولتاژ فازها و فرکانس برق شهر",
        icon = Icons.Outlined.Bolt,
        accentColor = p.cyan,
        trailing = {
            LtrText(
                text = snapshot.mainsFrequencyHz?.let { "%.2f Hz".format(it) } ?: "— Hz",
                style = MaterialTheme.typography.titleMedium,
                color = p.cyan,
            )
        },
    ) {
        ThreePhaseVoltageBars(voltages = snapshot.voltages)
    }
}

@Composable
private fun EmptyConnectionState(
    uiState: MonitoringUiState,
    canOpenGatewaySettings: Boolean,
    onConnect: () -> Unit,
    onOpenGatewaySettings: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val conn = uiState.connectionState
    val infinite = rememberInfiniteTransition(label = "empty_crest")
    val outerOrbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
        label = "empty_outer",
    )
    val innerOrbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (conn is ConnectionState.Connecting) 2_200 else 6_000, easing = LinearEasing)),
        label = "empty_inner",
    )
    val breath by infinite.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "empty_breath",
    )
    val shimmer by infinite.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(tween(3_800, easing = FastOutSlowInEasing)),
        label = "empty_shimmer",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (conn) {
            is ConnectionState.Error -> Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(p.faultBg)
                    .border(1.dp, p.fault.copy(alpha = 0.55f), RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = p.fault,
                    modifier = Modifier.size(44.dp),
                )
            }
            else -> Box(contentAlignment = Alignment.Center) {
                DgpAnimatedCrest(
                    size = 190.dp,
                    logoSize = 156.dp,
                    outerAngle = outerOrbit,
                    innerAngle = innerOrbit,
                    breathGlow = breath,
                    shimmerSweep = shimmer,
                    activeTracer = true,
                )
                if (conn is ConnectionState.Connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(164.dp),
                        color = p.gold.copy(alpha = 0.72f),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        when (conn) {
            is ConnectionState.Connecting -> {
                Text("در حال اتصال به کنترلر…", style = MaterialTheme.typography.headlineSmall, color = p.textPrimary)
                Spacer(Modifier.height(6.dp))
                if (!conn.isSimulated) {
                    LtrText(conn.target.substringBefore(" ("), style = MonoStyle, color = p.gold)
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    "لطفاً چند لحظه صبر کنید تا اطلاعات ایستگاه دریافت شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            is ConnectionState.Error -> {
                Text("عدم برقراری ارتباط", style = MaterialTheme.typography.headlineSmall, color = p.fault)
                Spacer(Modifier.height(10.dp))
                NoticeBox(
                    text = "اتصال به مبدل شبکه برقرار نشد. از اتصال گوشی به وای‌فای شبکه محلی و روشن بودن مبدل اطمینان حاصل کنید.",
                    tone = BadgeTone.FAULT,
                )
                if (conn.retryInSeconds != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "تلاش مجدد خودکار تا ${conn.retryInSeconds} ثانیه دیگر…",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textSecondary,
                    )
                }
                if (canOpenGatewaySettings) {
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(
                        onClick = onOpenGatewaySettings,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.9.dp, p.gold.copy(alpha = 0.55f)),
                    ) {
                        Icon(Icons.Outlined.SettingsEthernet, contentDescription = null, tint = p.gold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("تنظیمات اتصال مبدل", color = p.gold)
                    }
                }
            }

            else -> {
                Text(
                    "کنترلر بوستر پمپ DGP",
                    style = MaterialTheme.typography.headlineSmall,
                    color = p.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                val desc = when (uiState.savedConfig.mode) {
                    DataSourceMode.SIMULATION ->
                        "حالت نمایشی فعال است. برای مشاهده فشار، وضعیت پمپ‌ها و تنظیمات دکمه «شروع اتصال» را بزنید."
                    DataSourceMode.LIVE -> if (uiState.savedConfig.host.isBlank()) {
                        if (canOpenGatewaySettings) {
                            "برای اتصال به دستگاه، ابتدا آدرس IP مبدل شبکه را در بخش «اتصال» وارد کنید."
                        } else {
                            "آدرس مبدل شبکه هنوز تنظیم نشده است. لطفاً با مدیر سیستم هماهنگ کنید."
                        }
                    } else {
                        "آماده اتصال به کنترلر در آدرس ${uiState.savedConfig.host}:${uiState.savedConfig.port}"
                    }
                }
                Text(
                    desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DgpGoldButton(
                        text = "شروع اتصال",
                        icon = Icons.Outlined.Cable,
                        onClick = onConnect,
                        height = 46.dp,
                    )
                    if (canOpenGatewaySettings) {
                        OutlinedButton(
                            onClick = onOpenGatewaySettings,
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(0.9.dp, p.panelHighlight),
                            modifier = Modifier.height(46.dp),
                        ) {
                            Icon(Icons.Outlined.SettingsEthernet, contentDescription = null, tint = p.textPrimary, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("تنظیمات اتصال", color = p.textPrimary)
                        }
                    }
                }
            }
        }
    }
}
