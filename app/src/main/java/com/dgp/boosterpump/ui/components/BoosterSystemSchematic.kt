package com.dgp.boosterpump.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.controller.Bpc02Catalog
import com.dgp.boosterpump.controller.ControllerSnapshot
import com.dgp.boosterpump.controller.PumpState
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.ui.theme.MonoStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dynamic, real-time SCADA Booster Pump System P&ID Schematic guided by the official DGP BPC-02
 * faceplate layout and reference HMI diagrams:
 *
 *  [Water Tank + H/L/C Float Switch] ──▶ [Suction Header + Isolation Valve]
 *                                            ├──▲── [Pump 1] ──▲──┤
 *                                            ├──▲── [Pump 2] ──▲──┤  (dynamically adapts 1..6 pumps)
 *                                            └──▲── [Pump N] ──▲──┘
 *                                       [Discharge Collector Manifold] ──▶ [Pressure Gauge & OUT]
 *
 * Automatically adapts to:
 *  - Configured pump count (`snapshot.visiblePumps`: 1, 2, 3, 4, 5, or 6 pumps with zero overlap)
 *  - Selected controller Wiring Mode (`Mode 1`, `Mode 2`, `Mode 3`) & Drive count (`0..2` VFD vs DOL)
 *  - Individual pump live states (Running / Standby / Service 'S' / Fault)
 *  - Live Water Tank level & Floater H / L electrode states
 *  - Live Discharge System Pressure & Setpoint dial gauge
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicBoosterSystemSchematic(
    snapshot: ControllerSnapshot,
    modifier: Modifier = Modifier,
) {
    val p = LocalScadaPalette.current
    val pumps = snapshot.visiblePumps
    val pumpCount = pumps.size.coerceIn(1, 6)
    val runningCount = snapshot.runningPumpCount
    val hasActiveAlarms = snapshot.alarms.isNotEmpty()
    val lowWater = snapshot.floaterLowConnected == false
    val isFlowing = runningCount > 0 && !lowWater

    val mode = snapshot.wiringMode
    val driveCount = snapshot.variablePumpCount ?: 0

    // Animated water level inside tank driven by Floater H & L electrodes
    val targetLevel = when {
        snapshot.floaterLowConnected == true && snapshot.floaterHighConnected == true -> 0.80f
        snapshot.floaterLowConnected == true && snapshot.floaterHighConnected == false -> 0.46f
        snapshot.floaterLowConnected == false && snapshot.floaterHighConnected == false -> 0.14f
        snapshot.floaterLowConnected == false && snapshot.floaterHighConnected == true -> 0.52f
        else -> 0.0f
    }
    val tankLevel by animateFloatAsState(
        targetValue = targetLevel,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "schematic_tank_level",
    )

    val maxBar = maxOf(10.0, (snapshot.pressureSetpointBar ?: 0.0) + 2.0, (snapshot.pressureBar ?: 0.0) + 1.5)
    val pressureFrac by animateFloatAsState(
        targetValue = ((snapshot.pressureBar ?: 0.0) / maxBar).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "schematic_pressure_frac",
    )

    val flowDuration = when {
        runningCount >= 3 -> 900
        runningCount == 2 -> 1250
        runningCount == 1 -> 1650
        else -> 2800
    }
    val infinite = rememberInfiniteTransition(label = "schematic_anim")
    val flowPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(flowDuration, easing = LinearEasing)),
        label = "schematic_flow_phase",
    )
    val impellerAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(920, easing = LinearEasing)),
        label = "schematic_impeller_rot",
    )
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "schematic_wave",
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "schematic_pulse",
    )

    val waterColor = when {
        lowWater -> p.fault
        snapshot.floaterHighConnected == false -> p.warning
        else -> p.cyan
    }
    val pipeFlowColor = when {
        lowWater -> p.fault
        isFlowing -> p.cyan
        else -> p.standby
    }
    val stageBorderColor = when {
        hasActiveAlarms || lowWater -> p.fault.copy(alpha = 0.48f)
        isFlowing -> p.cyan.copy(alpha = 0.42f)
        else -> p.panelBorder
    }

    val (stationStateText, stationStateTone) = when {
        lowWater -> "Floater Error" to BadgeTone.FAULT
        hasActiveAlarms -> "Fault Active" to BadgeTone.FAULT
        snapshot.controlStatus == 0 -> "System Stopped" to BadgeTone.WARNING
        isFlowing -> "Pumping ($runningCount/$pumpCount)" to BadgeTone.OK
        else -> "Standby" to BadgeTone.INFO
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 1. Architecture & Operating State Badges Strip (LTR technical terminology)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusBadge(
                    text = Bpc02Catalog.wiringModeLabel(mode),
                    tone = BadgeTone.GOLD,
                )
                StatusBadge(
                    text = if (pumpCount == 1) "1 Pump" else "$pumpCount Pumps",
                    tone = BadgeTone.INFO,
                )
                StatusBadge(
                    text = Bpc02Catalog.driverCountLabel(snapshot.variablePumpCount),
                    tone = if (driveCount > 0) BadgeTone.GOLD else BadgeTone.NEUTRAL,
                )
                StatusBadge(
                    text = stationStateText,
                    tone = stationStateTone,
                    pulsing = hasActiveAlarms || lowWater || isFlowing,
                )
            }
        }

        // 2. Main Left-to-Right P&ID Schematic Stage
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.terminalBg)
                    .border(1.dp, stageBorderColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(186.dp),
                ) {
                    val w = size.width
                    val h = size.height

                    // Horizontal zone layout proportions (guaranteed zero overlap for 1..6 pumps!)
                    val tankLeft = w * 0.02f
                    val tankW = w * 0.19f
                    val tankTop = h * 0.22f
                    val tankH = h * 0.68f
                    val tankBottom = tankTop + tankH
                    val tankRight = tankLeft + tankW

                    val pumpZoneLeft = w * 0.28f
                    val pumpZoneRight = w * 0.76f
                    val pumpZoneW = pumpZoneRight - pumpZoneLeft

                    val suctionY = h * 0.80f
                    val dischargeY = h * 0.22f
                    val pumpMidY = (suctionY + dischargeY) * 0.52f

                    val gaugeCenterX = w * 0.89f
                    val gaugeCenterY = h * 0.52f
                    val gaugeR = minOf(w * 0.088f, h * 0.21f).coerceIn(22.dp.toPx(), 32.dp.toPx())

                    val pipeOuterThick = 8.5.dp.toPx()
                    val pipeInnerThick = 5.0.dp.toPx()
                    val pipeWallColor = if (p.isDark) Color(0xFF3A4459) else Color(0xFF8B99B0)
                    val pipeCoreColor = if (p.isDark) Color(0xFF141A26) else Color(0xFFDCE4EF)

                    // Compute exact center X for each configured pump (1..6)
                    val slotW = pumpZoneW / pumpCount.toFloat()
                    val pumpCentersX = List(pumpCount) { i ->
                        pumpZoneLeft + slotW * (i + 0.5f)
                    }
                    val lastPumpX = pumpCentersX.last()
                    val firstPumpX = pumpCentersX.first()

                    // ---- A. WATER STORAGE TANK + INLET PIPE + FLOAT SWITCH ELECTRODES ----
                    // Top-left water inlet fill pipe
                    val fillPipeX = tankLeft + tankW * 0.24f
                    drawLine(
                        color = pipeWallColor,
                        start = Offset(fillPipeX, h * 0.05f),
                        end = Offset(fillPipeX, tankTop + 2f),
                        strokeWidth = 5.5.dp.toPx(),
                        cap = StrokeCap.Butt,
                    )
                    drawLine(
                        color = waterColor.copy(alpha = 0.55f),
                        start = Offset(fillPipeX, h * 0.05f),
                        end = Offset(fillPipeX, tankTop + 2f),
                        strokeWidth = 2.6.dp.toPx(),
                    )
                    // Small inlet flange lip
                    drawLine(
                        color = pipeWallColor,
                        start = Offset(fillPipeX - 5.dp.toPx(), h * 0.05f),
                        end = Offset(fillPipeX + 5.dp.toPx(), h * 0.05f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )

                    // Tank body recessed vessel
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            if (p.isDark) {
                                listOf(Color(0xFF141925), Color(0xFF1E2638), Color(0xFF121722))
                            } else {
                                listOf(Color(0xFFDCE4F0), Color(0xFFF3F6FA), Color(0xFFD3DCEB))
                            },
                            startX = tankLeft,
                            endX = tankRight,
                        ),
                        topLeft = Offset(tankLeft, tankTop),
                        size = Size(tankW, tankH),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                    )

                    // Animated water fill inside tank
                    if (tankLevel > 0.01f) {
                        val waterTop = tankBottom - (tankH - 4.dp.toPx()) * tankLevel
                        val waterPath = Path().apply {
                            moveTo(tankLeft + 2.dp.toPx(), tankBottom - 2.dp.toPx())
                            lineTo(tankLeft + 2.dp.toPx(), waterTop)
                            val steps = 16
                            val usableW = tankW - 4.dp.toPx()
                            for (s in 0..steps) {
                                val x = tankLeft + 2.dp.toPx() + usableW * s / steps
                                val amp = if (tankLevel < 0.22f) 1.6f else 2.8f
                                val y = waterTop + sin(wavePhase + s * 0.55f) * amp
                                lineTo(x, y)
                            }
                            lineTo(tankRight - 2.dp.toPx(), tankBottom - 2.dp.toPx())
                            close()
                        }
                        drawPath(
                            path = waterPath,
                            brush = Brush.verticalGradient(
                                0f to waterColor.copy(alpha = 0.78f),
                                1f to waterColor.copy(alpha = 0.28f),
                                startY = waterTop,
                                endY = tankBottom,
                            ),
                        )
                    }

                    // Dashed High (H) & Low (L) level reference lines inside tank (matching DGP faceplate!)
                    val highLineY = tankTop + tankH * 0.30f
                    val lowLineY = tankTop + tankH * 0.74f
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
                    drawLine(
                        color = if (snapshot.floaterHighConnected == true) p.ok.copy(alpha = 0.85f) else p.textMuted.copy(alpha = 0.45f),
                        start = Offset(tankLeft + 3.dp.toPx(), highLineY),
                        end = Offset(tankRight - 3.dp.toPx(), highLineY),
                        strokeWidth = 1.1.dp.toPx(),
                        pathEffect = dashEffect,
                    )
                    drawLine(
                        color = if (snapshot.floaterLowConnected == true) p.ok.copy(alpha = 0.85f) else p.fault.copy(alpha = 0.90f),
                        start = Offset(tankLeft + 3.dp.toPx(), lowLineY),
                        end = Offset(tankRight - 3.dp.toPx(), lowLineY),
                        strokeWidth = 1.1.dp.toPx(),
                        pathEffect = dashEffect,
                    )

                    // 3 Vertical Float Switch Electrode Rods (H, L, C) from top-right of tank (BPC-02 faceplate)
                    val rodH_X = tankRight - tankW * 0.36f
                    val rodL_X = tankRight - tankW * 0.23f
                    val rodC_X = tankRight - tankW * 0.11f
                    val rodTopY = tankTop - 5.dp.toPx()
                    val rodColor = if (p.isDark) Color(0xFFB5C0D4) else Color(0xFF475569)

                    // Common electrode (deepest)
                    drawLine(rodColor.copy(alpha = 0.75f), Offset(rodC_X, rodTopY), Offset(rodC_X, tankBottom - 5.dp.toPx()), strokeWidth = 1.2.dp.toPx())
                    // Low electrode + sensor tip LED
                    drawLine(rodColor.copy(alpha = 0.85f), Offset(rodL_X, rodTopY), Offset(rodL_X, lowLineY), strokeWidth = 1.3.dp.toPx())
                    val lowDotColor = when (snapshot.floaterLowConnected) {
                        true -> p.ok
                        false -> p.fault
                        null -> p.standby
                    }
                    if (lowWater) {
                        drawCircle(p.fault.copy(alpha = 0.35f * pulse), radius = 6.5.dp.toPx(), center = Offset(rodL_X, lowLineY))
                    }
                    drawCircle(lowDotColor, radius = 3.2.dp.toPx(), center = Offset(rodL_X, lowLineY))

                    // High electrode + sensor tip LED
                    drawLine(rodColor.copy(alpha = 0.85f), Offset(rodH_X, rodTopY), Offset(rodH_X, highLineY), strokeWidth = 1.3.dp.toPx())
                    val highDotColor = when (snapshot.floaterHighConnected) {
                        true -> p.ok
                        false -> p.warning
                        null -> p.standby
                    }
                    drawCircle(highDotColor, radius = 3.2.dp.toPx(), center = Offset(rodH_X, highLineY))

                    // Tank border outline
                    drawRoundRect(
                        color = if (lowWater) p.fault.copy(alpha = 0.85f) else p.panelBorder,
                        topLeft = Offset(tankLeft, tankTop),
                        size = Size(tankW, tankH),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = Stroke(width = 1.6.dp.toPx()),
                    )

                    // ---- B. BOTTOM SUCTION HEADER PIPE + INLET ISOLATION VALVE ----
                    drawPipeSegment(
                        start = Offset(tankRight, suctionY),
                        end = Offset(lastPumpX + 6.dp.toPx(), suctionY),
                        outerThick = pipeOuterThick,
                        innerThick = pipeInnerThick,
                        wallColor = pipeWallColor,
                        coreColor = pipeCoreColor,
                    )
                    // Inlet isolation valve symbol between tank and pump 1 (matching DGP BPC-02 faceplate!)
                    val valveX = tankRight + (pumpZoneLeft - tankRight) * 0.50f
                    val valveR = 5.5.dp.toPx()
                    val valveTri = Path().apply {
                        moveTo(valveX - valveR, suctionY - valveR)
                        lineTo(valveX + valveR, suctionY + valveR)
                        lineTo(valveX + valveR, suctionY - valveR)
                        lineTo(valveX - valveR, suctionY + valveR)
                        close()
                    }
                    drawPath(valveTri, color = p.gold.copy(alpha = 0.85f))
                    // Valve lever handle
                    drawLine(
                        color = p.gold,
                        start = Offset(valveX, suctionY - valveR),
                        end = Offset(valveX, suctionY - valveR - 6.dp.toPx()),
                        strokeWidth = 1.6.dp.toPx(),
                    )
                    drawLine(
                        color = p.gold,
                        start = Offset(valveX - 2.dp.toPx(), suctionY - valveR - 6.dp.toPx()),
                        end = Offset(valveX + 8.dp.toPx(), suctionY - valveR - 6.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )

                    // Animated suction flow pulses along bottom header up to the furthest running pump
                    val lastRunningIdx = pumps.indexOfLast { it.running == true && it.enabled != false && it.failed != true }
                    if (isFlowing && lastRunningIdx >= 0) {
                        val flowEndX = pumpCentersX[lastRunningIdx]
                        drawFlowPulses(
                            start = Offset(tankRight + 4.dp.toPx(), suctionY),
                            end = Offset(flowEndX, suctionY),
                            phase = flowPhase,
                            color = pipeFlowColor,
                            pulseCount = 4,
                        )
                    }

                    // ---- C. TOP DISCHARGE COLLECTOR MANIFOLD -> PRESSURE GAUGE -> OUTLET ----
                    val dischargeEndX = w * 0.97f
                    drawPipeSegment(
                        start = Offset(firstPumpX - 6.dp.toPx(), dischargeY),
                        end = Offset(dischargeEndX, dischargeY),
                        outerThick = pipeOuterThick,
                        innerThick = pipeInnerThick,
                        wallColor = pipeWallColor,
                        coreColor = pipeCoreColor,
                    )
                    // Outlet directional arrow chevron at right end of discharge collector
                    val arrowX = dischargeEndX - 1.dp.toPx()
                    val arrowPath = Path().apply {
                        moveTo(arrowX - 6.dp.toPx(), dischargeY - 5.dp.toPx())
                        lineTo(arrowX + 1.dp.toPx(), dischargeY)
                        lineTo(arrowX - 6.dp.toPx(), dischargeY + 5.dp.toPx())
                    }
                    drawPath(
                        path = arrowPath,
                        color = if (isFlowing) p.gold else p.textMuted,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    if (isFlowing && lastRunningIdx >= 0) {
                        val firstRunningIdx = pumps.indexOfFirst { it.running == true && it.enabled != false && it.failed != true }
                        val dischargeStartX = pumpCentersX[firstRunningIdx.coerceAtLeast(0)]
                        drawFlowPulses(
                            start = Offset(dischargeStartX, dischargeY),
                            end = Offset(dischargeEndX - 8.dp.toPx(), dischargeY),
                            phase = flowPhase,
                            color = p.gold,
                            pulseCount = 4,
                        )
                    }

                    // ---- D. MODE 2 / MODE 3 SHARED VFD CHANGEOVER BUS LINE (above discharge header) ----
                    if (mode in 2..3 && pumpCount >= 1) {
                        val busY = h * 0.08f
                        val busColor = when {
                            snapshot.driveFailed == true -> p.fault
                            snapshot.driveEnabled == false -> p.standby
                            snapshot.drStatus == true -> p.gold
                            else -> p.gold.copy(alpha = 0.45f)
                        }
                        if (pumpCount > 1) {
                            drawLine(
                                color = busColor,
                                start = Offset(firstPumpX, busY),
                                end = Offset(lastPumpX, busY),
                                strokeWidth = 1.6.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f),
                            )
                        }
                        pumpCentersX.forEach { cx ->
                            drawLine(
                                color = busColor.copy(alpha = 0.7f),
                                start = Offset(cx, busY),
                                end = Offset(cx, dischargeY - pipeOuterThick * 0.7f),
                                strokeWidth = 1.1.dp.toPx(),
                            )
                        }
                    }

                    // ---- E. DYNAMIC PARALLEL PUMPS (1..6) ----
                    val pumpR = when (pumpCount) {
                        1 -> 24.dp.toPx()
                        2 -> 21.dp.toPx()
                        3 -> 18.5.dp.toPx()
                        4 -> 16.dp.toPx()
                        else -> 13.5.dp.toPx()
                    }.coerceAtMost(slotW * 0.41f)

                    val riserOuter = (pipeOuterThick * 0.78f).coerceAtMost(pumpR * 0.55f)
                    val riserInner = (pipeInnerThick * 0.72f).coerceAtMost(pumpR * 0.34f)
                    val inactiveGray = if (p.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

                    pumps.forEachIndexed { idx, pump ->
                        val cx = pumpCentersX[idx]
                        val isFault = pump.failed == true
                        val isDisabled = pump.enabled == false && !isFault
                        val isRunning = !isFault && !isDisabled && pump.running == true
                        val isReady = !isFault && !isDisabled && !isRunning && pump.enabled == true
                        val isDedicatedVfd = mode == 1 && (idx + 1) <= driveCount

                        val casingColor = when {
                            isFault -> p.fault
                            isDisabled -> inactiveGray
                            isRunning -> p.ok
                            isReady -> p.cyan.copy(alpha = 0.85f)
                            else -> p.standby
                        }
                        val ledColor = when {
                            isFault -> p.fault
                            isDisabled -> p.warning
                            isRunning -> p.ok
                            isReady -> p.cyan
                            else -> p.standby
                        }

                        // Vertical suction riser (bottom header -> pump bottom)
                        drawPipeSegment(
                            start = Offset(cx, suctionY - pipeOuterThick * 0.35f),
                            end = Offset(cx, pumpMidY + pumpR - 1f),
                            outerThick = riserOuter,
                            innerThick = riserInner,
                            wallColor = if (isRunning) p.cyan.copy(alpha = 0.85f) else pipeWallColor,
                            coreColor = pipeCoreColor,
                        )
                        // Vertical discharge riser (pump top -> top collector)
                        drawPipeSegment(
                            start = Offset(cx, pumpMidY - pumpR + 1f),
                            end = Offset(cx, dischargeY + pipeOuterThick * 0.35f),
                            outerThick = riserOuter,
                            innerThick = riserInner,
                            wallColor = if (isRunning) p.ok.copy(alpha = 0.85f) else pipeWallColor,
                            coreColor = pipeCoreColor,
                        )
                        // Pipe flange bars on discharge riser (like Grundfos SCADA reference!)
                        val flangeY = dischargeY + pipeOuterThick * 0.95f
                        drawLine(
                            color = if (isRunning) p.gold else pipeWallColor,
                            start = Offset(cx - riserOuter * 0.85f, flangeY),
                            end = Offset(cx + riserOuter * 0.85f, flangeY),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )

                        // Upward flow pulses inside THIS pump's risers only when THIS pump is running!
                        if (isRunning && !lowWater) {
                            drawFlowPulses(
                                start = Offset(cx, suctionY - 2f),
                                end = Offset(cx, pumpMidY + pumpR),
                                phase = flowPhase,
                                color = p.cyan,
                                pulseCount = 2,
                            )
                            drawFlowPulses(
                                start = Offset(cx, pumpMidY - pumpR),
                                end = Offset(cx, dischargeY + 2f),
                                phase = flowPhase,
                                color = p.ok,
                                pulseCount = 2,
                            )
                        }

                        val center = Offset(cx, pumpMidY)

                        // Ambient active/fault halo
                        if (isRunning || isFault) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0f to casingColor.copy(alpha = 0.34f * pulse),
                                    1f to Color.Transparent,
                                    center = center,
                                    radius = pumpR * 1.55f,
                                ),
                                radius = pumpR * 1.55f,
                                center = center,
                            )
                        }

                        // Dedicated VFD gold outer ring in Mode 1
                        if (isDedicatedVfd) {
                            drawCircle(
                                color = p.gold.copy(alpha = if (isRunning) 0.90f else 0.48f),
                                radius = pumpR + 3.dp.toPx(),
                                center = center,
                                style = Stroke(width = 1.2.dp.toPx()),
                            )
                        }

                        // Volute body fill & ring
                        drawCircle(
                            brush = Brush.radialGradient(
                                0f to p.panel,
                                0.72f to p.panelElevated,
                                1f to casingColor.copy(alpha = 0.25f),
                                center = center,
                                radius = pumpR,
                            ),
                            radius = pumpR,
                            center = center,
                        )
                        drawCircle(
                            color = casingColor,
                            radius = pumpR,
                            center = center,
                            style = Stroke(width = (pumpR * 0.14f).coerceIn(1.6.dp.toPx(), 2.5.dp.toPx())),
                        )

                        // 5-blade curved centrifugal impeller
                        val rot = if (isRunning) impellerAngle else 0f
                        rotate(degrees = rot, pivot = center) {
                            val bladeStroke = (pumpR * 0.11f).coerceIn(1.2.dp.toPx(), 2.1.dp.toPx())
                            for (b in 0 until 5) {
                                rotate(degrees = b * 72f, pivot = center) {
                                    val blade = Path().apply {
                                        moveTo(center.x, center.y)
                                        quadraticTo(
                                            center.x + pumpR * 0.44f,
                                            center.y - pumpR * 0.16f,
                                            center.x + pumpR * 0.65f,
                                            center.y - pumpR * 0.40f,
                                        )
                                    }
                                    drawPath(
                                        path = blade,
                                        color = if (isRunning) p.cyan else casingColor.copy(alpha = 0.80f),
                                        style = Stroke(width = bladeStroke, cap = StrokeCap.Round),
                                    )
                                }
                            }
                        }
                        // Shaft hub
                        drawCircle(
                            color = if (isRunning) p.gold else casingColor,
                            radius = (pumpR * 0.20f).coerceAtLeast(2.2.dp.toPx()),
                            center = center,
                        )

                        // Top-right status LED beacon on each pump (inspired by reference HMI!)
                        val ledCenter = Offset(cx + pumpR * 0.72f, pumpMidY - pumpR * 0.72f)
                        val ledR = (pumpR * 0.23f).coerceIn(3.dp.toPx(), 4.8.dp.toPx())
                        if (isRunning || isFault) {
                            drawCircle(ledColor.copy(alpha = 0.35f * pulse), radius = ledR * 1.65f, center = ledCenter)
                        }
                        drawCircle(p.terminalBg, radius = ledR + 1.dp.toPx(), center = ledCenter)
                        drawCircle(ledColor, radius = ledR, center = ledCenter)
                    }

                    // ---- F. INTEGRATED PRESSURE GAUGE MOUNTED ON DISCHARGE HEADER (Right Zone) ----
                    // Vertical gauge tap stem connecting discharge collector to analog pressure dial
                    drawLine(
                        color = pipeWallColor,
                        start = Offset(gaugeCenterX, dischargeY + pipeOuterThick * 0.35f),
                        end = Offset(gaugeCenterX, gaugeCenterY - gaugeR),
                        strokeWidth = 3.2.dp.toPx(),
                    )
                    drawCircle(
                        color = p.gold.copy(alpha = 0.7f),
                        radius = 3.2.dp.toPx(),
                        center = Offset(gaugeCenterX, dischargeY),
                    )

                    val dialCenter = Offset(gaugeCenterX, gaugeCenterY)
                    // Dial face
                    drawCircle(color = p.panel, radius = gaugeR, center = dialCenter)
                    drawCircle(
                        color = if (hasActiveAlarms) p.fault.copy(alpha = 0.85f) else p.gold.copy(alpha = 0.75f),
                        radius = gaugeR,
                        center = dialCenter,
                        style = Stroke(width = 1.8.dp.toPx()),
                    )
                    // Dial arc track & active sweep (240 deg)
                    val arcInset = 4.5.dp.toPx()
                    val arcTopLeft = Offset(dialCenter.x - gaugeR + arcInset, dialCenter.y - gaugeR + arcInset)
                    val arcSize = Size((gaugeR - arcInset) * 2f, (gaugeR - arcInset) * 2f)
                    val dialStart = 150f
                    val dialSweep = 240f
                    drawArc(
                        color = p.panelElevated,
                        startAngle = dialStart,
                        sweepAngle = dialSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
                    )
                    if (snapshot.pressureBar != null && pressureFrac > 0.005f) {
                        val gaugeColor = when {
                            hasActiveAlarms -> p.fault
                            snapshot.pressureBar <= 0.5 -> p.warning
                            else -> p.cyan
                        }
                        drawArc(
                            color = gaugeColor,
                            startAngle = dialStart,
                            sweepAngle = dialSweep * pressureFrac,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                    // Gold setpoint tick on dial
                    snapshot.pressureSetpointBar?.takeIf { it > 0.0 }?.let { sp ->
                        val spFrac = (sp / maxBar).toFloat().coerceIn(0f, 1f)
                        val spRad = (dialStart + dialSweep * spFrac) * PI / 180.0
                        drawLine(
                            color = p.gold,
                            start = Offset(
                                dialCenter.x + ((gaugeR - 7.dp.toPx()) * cos(spRad)).toFloat(),
                                dialCenter.y + ((gaugeR - 7.dp.toPx()) * sin(spRad)).toFloat(),
                            ),
                            end = Offset(
                                dialCenter.x + ((gaugeR - 1.5.dp.toPx()) * cos(spRad)).toFloat(),
                                dialCenter.y + ((gaugeR - 1.5.dp.toPx()) * sin(spRad)).toFloat(),
                            ),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                    // Live moving dial needle
                    val needleDeg = dialStart + dialSweep * pressureFrac
                    val needleRad = needleDeg * PI / 180.0
                    val needleLen = gaugeR * 0.68f
                    drawLine(
                        color = if (hasActiveAlarms) p.fault else p.gold,
                        start = dialCenter,
                        end = Offset(
                            dialCenter.x + (needleLen * cos(needleRad)).toFloat(),
                            dialCenter.y + (needleLen * sin(needleRad)).toFloat(),
                        ),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawCircle(color = p.gold, radius = 3.dp.toPx(), center = dialCenter)
                }

                // 3. Synchronized Text Labels Row aligned 1:1 beneath Tank | Pumps (1..N) | Pressure Gauge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Left: Tank + Floater H/L summary
                    Column(
                        modifier = Modifier.weight(0.24f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Tank",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = p.textPrimary,
                            textAlign = TextAlign.Center,
                        )
                        val floaterShort = when {
                            snapshot.floaterLowConnected == true && snapshot.floaterHighConnected == true -> "H:1 L:1"
                            snapshot.floaterLowConnected == true && snapshot.floaterHighConnected == false -> "H:0 L:1"
                            snapshot.floaterLowConnected == false -> "LOW!"
                            else -> "H/L —"
                        }
                        Text(
                            text = floaterShort,
                            style = MonoStyle.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                            color = waterColor,
                            textAlign = TextAlign.Center,
                        )
                    }

                    // Center: Dynamic Pump Tags (P1..PN) aligned directly under each pump column!
                    Row(
                        modifier = Modifier.weight(0.50f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top,
                    ) {
                        pumps.forEachIndexed { idx, pump ->
                            val isFault = pump.failed == true
                            val isDisabled = pump.enabled == false && !isFault
                            val isRunning = !isFault && !isDisabled && pump.running == true
                            val tagColor = when {
                                isFault -> p.fault
                                isDisabled -> p.warning
                                isRunning -> p.ok
                                else -> p.textPrimary
                            }
                            val subTag = when {
                                isFault -> "ERR"
                                isDisabled -> "S"
                                mode == 1 && (idx + 1) <= driveCount -> "VFD"
                                mode == 1 -> "DOL"
                                isRunning -> "RUN"
                                else -> "RDY"
                            }
                            val subColor = when {
                                isFault -> p.fault
                                isDisabled -> p.warning
                                subTag == "VFD" -> p.gold
                                isRunning -> p.ok
                                else -> p.textMuted
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                Text(
                                    text = "P${pump.number}",
                                    style = MonoStyle.copy(
                                        fontSize = if (pumpCount >= 5) 9.5.sp else 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = tagColor,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    text = subTag,
                                    style = MonoStyle.copy(
                                        fontSize = if (pumpCount >= 5) 7.5.sp else 8.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = subColor,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    // Right: Live System Pressure & Setpoint readout aligned under Pressure Gauge
                    Column(
                        modifier = Modifier.weight(0.26f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = snapshot.pressureBar?.let { String.format(Locale.US, "%.2f Bar", it) } ?: "— Bar",
                            style = MonoStyle.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                            color = p.gold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = snapshot.pressureSetpointBar?.let { String.format(Locale.US, "SP %.2f", it) } ?: "SP —",
                            style = MonoStyle.copy(fontSize = 8.5.sp),
                            color = p.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // 4. Compact Visual Legend + Active Drive Frequency Readout (Intuitive for all users)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SchematicLegendDot(color = p.ok, label = "Running")
                SchematicLegendDot(color = p.cyan, label = "Standby")
                SchematicLegendDot(color = p.warning, label = "Service (S)")
                SchematicLegendDot(color = p.fault, label = "Fault")
                if (driveCount > 0 && snapshot.drive1FrequencyHz != null) {
                    val d1 = String.format(Locale.US, "%.1f Hz", snapshot.drive1FrequencyHz)
                    val driveText = if (driveCount >= 2 && snapshot.drive2FrequencyHz != null) {
                        "D1: $d1  |  D2: ${String.format(Locale.US, "%.1f Hz", snapshot.drive2FrequencyHz)}"
                    } else {
                        "Drive: $d1"
                    }
                    Surface(
                        color = p.goldSoft,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(0.7.dp, p.gold.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = driveText,
                            style = MonoStyle.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                            color = p.gold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SchematicLegendDot(color: Color, label: String) {
    val p = LocalScadaPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = p.textSecondary,
        )
    }
}

private fun DrawScope.drawPipeSegment(
    start: Offset,
    end: Offset,
    outerThick: Float,
    innerThick: Float,
    wallColor: Color,
    coreColor: Color,
) {
    drawLine(
        color = wallColor,
        start = start,
        end = end,
        strokeWidth = outerThick,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = coreColor,
        start = start,
        end = end,
        strokeWidth = innerThick,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawFlowPulses(
    start: Offset,
    end: Offset,
    phase: Float,
    color: Color,
    pulseCount: Int,
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    for (i in 0 until pulseCount) {
        val t = (phase + i.toFloat() / pulseCount) % 1f
        val tBack = (t - 0.14f).coerceAtLeast(0f)
        val fade = sin(t * PI).toFloat().coerceIn(0f, 1f)
        val p1 = Offset(start.x + dx * tBack, start.y + dy * tBack)
        val p2 = Offset(start.x + dx * t, start.y + dy * t)
        drawLine(
            color = color.copy(alpha = 0.92f * fade),
            start = p1,
            end = p2,
            strokeWidth = 3.0.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
