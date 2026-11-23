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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (conn is ConnectionState.Error) {
            item {
                NoticeBox(
                    text = "خطای ارتباط: ${conn.message} — آخرین مقادیر دریافت‌شده نمایش داده می‌شوند.",
                    tone = BadgeTone.FAULT,
                )
            }
        } else if (conn is ConnectionState.Connected && conn.partialError != null) {
            item {
                NoticeBox(
                    text = "برخی بلوک‌های خواندن با خطا مواجه شدند: ${conn.partialError}",
                    tone = BadgeTone.WARNING,
                )
            }
        }

        item { HeroTelemetryPanel(snapshot) }

        item {
            val p = LocalScadaPalette.current
            ScadaPanel(
                title = "شماتیک هیدرولیک ایستگاه بوستر",
                subtitle = "جریان زنده مکش، هسته پمپاژ و کلکتور تحت فشار",
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

        item { AlarmsPanel(snapshot) }

        item {
            val p = LocalScadaPalette.current
            ScadaPanel(
                title = "روند لحظه‌ای فشار کلکتور",
                subtitle = "نمودار تغییرات در ۴۰ نمونه‌برداری اخیر",
                icon = Icons.AutoMirrored.Outlined.ShowChart,
                accentColor = p.cyan,
            ) {
                PressureTrendChart(samples = uiState.pressureHistory)
            }
        }

        item { BoosterStationPumpsPanel(snapshot.pumps, onOpenPumps) }

        item { ElectricalAnalyzerPanel(snapshot) }
    }
}

@Composable
private fun HeroTelemetryPanel(snapshot: ControllerSnapshot) {
    val p = LocalScadaPalette.current
    ScadaPanel(
        title = "وضعیت اصلی بوستر پمپ",
        subtitle = "فشار کلکتور (ورد ۲) • سطح مخزن (کویل ۳۲–۳۳)",
        icon = Icons.Outlined.Speed,
        accentColor = p.gold,
        trailing = {
            val (label, tone) = when (snapshot.driveFailed) {
                true -> "خطای درایو" to BadgeTone.FAULT
                false -> "درایو سالم" to BadgeTone.OK
                null -> "درایو —" to BadgeTone.NEUTRAL
            }
            StatusBadge(label, tone, pulsing = snapshot.driveFailed == true)
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PressureArcGauge(
                pressureBar = snapshot.pressureBar,
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
            KpiRailCell(
                title = "فرکانس درایو",
                value = snapshot.driveFrequencyRaw?.toString() ?: "—",
                caption = "خام ۰–۱۶۰۰",
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
                title = "فرکانس شبکه",
                value = snapshot.mainsFrequencyHz?.let { "%.2f Hz".format(it) } ?: "—",
                caption = "دقت ۰.۰۱ هرتز",
                accent = p.cyan,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .width(0.8.dp)
                    .height(40.dp)
                    .background(p.hairline),
            )
            KpiRailCell(
                title = "پمپ فعال",
                value = "${snapshot.runningPumpCount} / 6",
                caption = "صفحه LCD: ${snapshot.displayedPage ?: "—"}",
                accent = if (snapshot.runningPumpCount > 0) p.ok else p.gold,
                modifier = Modifier.weight(0.95f),
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
private fun AlarmsPanel(snapshot: ControllerSnapshot) {
    val p = LocalScadaPalette.current
    val alarms = snapshot.alarms
    val hasAlarms = alarms.isNotEmpty()
    ScadaPanel(
        title = "وضعیت حفاظت و آلارم‌ها",
        subtitle = "کلمه خطا (ورد ۸۰: ${snapshot.systemErrorWord?.let { "0x%04X".format(it) } ?: "—"}) و بیت‌های ۲۵–۳۱",
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
                    "تمام حفاظت‌های بوستر پمپ، کنترل فاز و درایو در وضعیت پایدار هستند.",
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
            }
        }
    }
}

@Composable
private fun BoosterStationPumpsPanel(pumps: List<PumpState>, onOpenPumps: () -> Unit) {
    val p = LocalScadaPalette.current
    val running = pumps.count { it.running == true }
    val failed = pumps.count { it.failed == true }
    ScadaPanel(
        title = "وضعیت پمپ‌ها ($running در حال کار)",
        subtitle = if (failed > 0) "$failed پمپ دارای خطا است • کویل‌های ۱۹–۳۱" else "چرخش زنده پروانه و تایمر کارکرد پمپ‌های ۱–۴",
        icon = Icons.Outlined.WaterDrop,
        accentColor = if (failed > 0) p.fault else p.ok,
        trailing = {
            Surface(
                onClick = onOpenPumps,
                color = p.panelElevated,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(0.8.dp, p.panelBorder),
            ) {
                Text(
                    "جزئیات کامل",
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
            }
        }
    }
}

@Composable
private fun PumpHmiTile(pump: PumpState, modifier: Modifier = Modifier) {
    val p = LocalScadaPalette.current
    val borderColor = when {
        pump.failed == true -> p.fault.copy(alpha = 0.55f)
        pump.running == true -> p.ok.copy(alpha = 0.42f)
        else -> p.panelBorder
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(p.panelElevated)
            .border(0.9.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedImpellerIcon(running = pump.running, failed = pump.failed, size = 38.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("پمپ ${pump.number}", style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
            val (label, tone) = when {
                pump.failed == true -> "خطا" to BadgeTone.FAULT
                pump.running == true -> "در حال کار" to BadgeTone.OK
                pump.running == false -> "توقف" to BadgeTone.NEUTRAL
                else -> "نامشخص" to BadgeTone.NEUTRAL
            }
            StatusBadge(label, tone, pulsing = pump.failed == true)
            LtrText(
                text = when {
                    pump.runTime != null -> PersianLabels.formatRunTimeCompact(pump.runTime)
                    pump.hasRunTimeRegisters -> "--:--:--"
                    else -> "بدون تایمر"
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
        title = "آنالایزر برق سه‌فاز و فرکانس",
        subtitle = "ولتاژهای خط و فاز (ورد ۳–۸) • فرکانس شبکه (ورد ۹)",
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
                Text("در حال برقراری ارتباط…", style = MaterialTheme.typography.headlineSmall, color = p.textPrimary)
                Spacer(Modifier.height(6.dp))
                LtrText(conn.target, style = MonoStyle, color = p.gold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "ایجاد نشست مدباس ایمن با کنترلر DGP BPC02",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            is ConnectionState.Error -> {
                Text("خطا در برقراری ارتباط", style = MaterialTheme.typography.headlineSmall, color = p.fault)
                Spacer(Modifier.height(10.dp))
                NoticeBox(text = conn.message, tone = BadgeTone.FAULT)
                if (conn.retryInSeconds != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "تلاش مجدد خودکار تا ${conn.retryInSeconds} ثانیه دیگر…",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textSecondary,
                    )
                }
                Spacer(Modifier.height(18.dp))
                OutlinedButton(
                    onClick = onOpenGatewaySettings,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.9.dp, p.gold.copy(alpha = 0.55f)),
                ) {
                    Icon(Icons.Outlined.SettingsEthernet, contentDescription = null, tint = p.gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("بررسی تنظیمات مبدل ES 487", color = p.gold)
                }
            }

            else -> {
                Text(
                    "کنترل و مانیتورینگ بوستر پمپ DGP",
                    style = MaterialTheme.typography.headlineSmall,
                    color = p.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                val desc = when (uiState.savedConfig.mode) {
                    DataSourceMode.SIMULATION ->
                        "حالت شبیه‌سازی انتخاب شده است. برای مشاهده گیج فشار، شماتیک هیدرولیک، پمپ‌ها و آلارم‌ها دکمه «شروع اتصال» را بزنید."
                    DataSourceMode.LIVE -> if (uiState.savedConfig.host.isBlank()) {
                        "حالت اتصال واقعی فعال است — ابتدا آدرس IP مبدل سمین ES 487 را در تب «مبدل» وارد کنید."
                    } else {
                        "آماده اتصال به مبدل سمین ES 487 در آدرس ${uiState.savedConfig.host}:${uiState.savedConfig.port} (شناسه Slave: ${uiState.savedConfig.unitId})."
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
                    OutlinedButton(
                        onClick = onOpenGatewaySettings,
                        shape = RoundedCornerShape(13.dp),
                        border = BorderStroke(0.9.dp, p.panelHighlight),
                        modifier = Modifier.height(46.dp),
                    ) {
                        Icon(Icons.Outlined.SettingsEthernet, contentDescription = null, tint = p.textPrimary, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("تنظیمات مبدل", color = p.textPrimary)
                    }
                }
            }
        }
    }
}
