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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.controller.Bpc02Catalog
import com.dgp.boosterpump.controller.ControllerSnapshot
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.ui.theme.MonoStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Computes a smooth, physically grounded hydraulic flow velocity factor in `0f..1f`
 * based on active pumps, live inverter frequency (`drive1FrequencyHz` / `drive2FrequencyHz`),
 * wiring mode, and tank water availability.
 *
 * - Returns `0f` when no pumps are running or low-water protection is active.
 * - Scales proportionally with live VFD frequency (0..50 Hz) and additional DOL pumps so pipe
 *   flow velocity and impeller RPM respond realistically and continuously to station load.
 */
fun computeHydraulicFlowSpeedFactor(snapshot: ControllerSnapshot): Float {
    val lowWater = snapshot.floaterLowConnected == false
    if (lowWater) return 0f

    val pumps = snapshot.visiblePumps
    val runningIndices = pumps.mapIndexedNotNull { idx, pump ->
        if (pump.failed != true && pump.enabled != false && pump.running == true) idx else null
    }
    if (runningIndices.isEmpty()) return 0f

    val mode = snapshot.wiringMode ?: 2
    val driveCount = (snapshot.variablePumpCount ?: 0).coerceIn(0, 2)
    val d1Ratio = snapshot.drive1FrequencyHz?.let { (it / 50.0).toFloat().coerceIn(0.18f, 1.0f) }
    val d2Ratio = snapshot.drive2FrequencyHz?.let { (it / 50.0).toFloat().coerceIn(0.18f, 1.0f) }

    var cumulativeLoad = 0f
    runningIndices.forEachIndexed { activeOrder, pumpIdx ->
        val pumpSpeed = when {
            driveCount == 0 -> 1.0f // Fixed-speed DOL
            mode == 1 -> when {
                pumpIdx == 0 && driveCount >= 1 -> d1Ratio ?: 0.72f
                pumpIdx == 1 && driveCount >= 2 -> d2Ratio ?: d1Ratio ?: 0.72f
                else -> 1.0f // Remaining pumps are DOL
            }
            else -> {
                // Mode 2 & 3: 1 shared drive regulates the lead/variable pump; additional pumps run DOL
                if (activeOrder == 0) d1Ratio ?: 0.72f else 1.0f
            }
        }
        cumulativeLoad += pumpSpeed
    }

    // Smooth perceptual mapping:
    // 1 pump @ 25 Hz -> ~0.34 | 1 pump @ 38.3 Hz -> ~0.48 | 1 pump @ 50 Hz -> ~0.58 |
    // 2 pumps -> ~0.82 | 3+ pumps -> 1.00
    val maxExpectedLoad = pumps.size.coerceIn(1, 3).toFloat()
    val normalized = (cumulativeLoad / maxExpectedLoad).coerceIn(0f, 1f)
    return (0.22f + 0.78f * normalized).coerceIn(0f, 1f)
}

/**
 * Dynamic, real-time SCADA Booster Pump System P&ID Schematic:
 *
 *  [Water Tank + H/L/C Electrodes] ──▶ [Isolation Valve] ──▶ [Continuous Suction Manifold]
 *                                                                 ├──▲── [Pump 1..N] ──▲──┤
 *                                                          [Continuous Discharge Collector] ──▶ [Pressure Dial] ──▶ OUT
 *
 * Key hydraulic & visual characteristics:
 * - True 2-pass continuous piping (outer metallic walls first, unbroken blue water interior on top)
 *   so every T-junction, 90° elbow, and tank outlet forms a real, seamless hydraulic path with zero dead ends.
 * - Unified hydraulic blue liquid across all pipes (suction header, vertical risers, discharge collector).
 * - Continuous frame-synced flow & impeller animation (`withFrameNanos`) driven smoothly by live Drive Hz
 *   and active pump count with zero phase jumps when frequency changes.
 * - Compact circular Pressure Dial with live `Bar` and `SP` values centered cleanly inside the dial.
 * - Tightly aligned `Tank (H/L)`, `P1..PN (RUN/RDY/S/ERR)`, continuous live `Drive: XX.X Hz` bar under
 *   the pumps, and a complete color-coded legend explicitly defining `RUN`, `RDY`, `S`, and `ERR`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicBoosterSystemSchematic(
    snapshot: ControllerSnapshot,
    modifier: Modifier = Modifier,
    showArchitectureBadges: Boolean = true,
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

    // Continuous frame-synced physics accumulator: speed adapts smoothly to live Drive Hz + active pumps
    // without EVER restarting or stuttering animation phase.
    val targetSpeedFactor by rememberUpdatedState(computeHydraulicFlowSpeedFactor(snapshot))
    val targetImpellerSpeed by rememberUpdatedState(
        if (!isFlowing) {
            0f
        } else {
            val d1 = snapshot.drive1FrequencyHz?.let { (it / 50.0).toFloat().coerceIn(0.25f, 1.0f) } ?: 0.80f
            val base = if (driveCount > 0 && runningCount == 1) d1 else (0.55f + 0.45f * targetSpeedFactor)
            base.coerceIn(0.25f, 1.0f)
        },
    )
    var smoothedFlowSpeed by remember { mutableFloatStateOf(0f) }
    var flowOffsetDp by remember { mutableFloatStateOf(0f) }
    var impellerAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    // Exponential velocity smoothing (realistic hydraulic inertia)
                    val lerpAlpha = (dt * 4.5f).coerceIn(0f, 1f)
                    smoothedFlowSpeed += (targetSpeedFactor - smoothedFlowSpeed) * lerpAlpha

                    if (smoothedFlowSpeed > 0.005f) {
                        // Continuous marching stream speed: 24..96 dp/sec proportional to hydraulic load
                        val dpPerSec = 20f + smoothedFlowSpeed * 78f
                        flowOffsetDp = (flowOffsetDp + dpPerSec * dt) % 1000f
                    }
                    if (targetImpellerSpeed > 0.005f) {
                        // Impeller rotation: 140..420 deg/sec proportional to VFD frequency / load
                        val degPerSec = 110f + targetImpellerSpeed * 310f
                        impellerAngle = (impellerAngle + degPerSec * dt) % 360f
                    }
                }
                lastNanos = frameNanos
            }
        }
    }

    val infinite = rememberInfiniteTransition(label = "schematic_ambient")
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

    // Unified hydraulic water blue palette (tank + all pipes)
    val tankWaterColor = when {
        lowWater -> p.fault
        snapshot.floaterHighConnected == false -> p.warning
        else -> Color(0xFF38BDF8)
    }
    val staticPipeWaterColor = when {
        lowWater -> p.fault.copy(alpha = 0.35f)
        p.isDark -> Color(0xFF0E4A73)
        else -> Color(0xFF38BDF8).copy(alpha = 0.55f)
    }
    val activePipeWaterBase = when {
        lowWater -> p.fault.copy(alpha = 0.55f)
        else -> Color(0xFF0284C7)
    }
    val activeFlowStreamColor = when {
        lowWater -> p.fault
        else -> Color(0xFF7DD3FC)
    }
    val stageBorderColor = when {
        hasActiveAlarms || lowWater -> p.fault.copy(alpha = 0.52f)
        isFlowing -> Color(0xFF38BDF8).copy(alpha = 0.46f)
        else -> p.panelBorder
    }

    val (stationStateText, stationStateTone) = when {
        lowWater -> "Floater Error" to BadgeTone.FAULT
        hasActiveAlarms -> "Fault Active" to BadgeTone.FAULT
        snapshot.controlStatus == 0 -> "System Stopped" to BadgeTone.WARNING
        isFlowing -> "Pumping ($runningCount/$pumpCount)" to BadgeTone.OK
        else -> "Standby" to BadgeTone.INFO
    }

    val activeDriveFreqHz = snapshot.drive1FrequencyHz
    val animatedDriveFrac by animateFloatAsState(
        targetValue = ((activeDriveFreqHz ?: 0.0) / 50.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "schematic_drive_bar_frac",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Optional Architecture & Operating State Badges Strip (hidden when merged inside Hero card)
        if (showArchitectureBadges) {
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
        }

        // Main Left-to-Right Recessed Glass P&ID Schematic Stage
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.terminalBg)
                    .border(1.05.dp, stageBorderColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Canvas + Precision Pressure Dial Value Overlay (aligned mathematically via BoxWithConstraints)
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp),
                ) {
                    val stageW = maxWidth
                    val stageH = maxHeight

                    val gaugeCenterXFraction = 0.875f
                    val gaugeCenterYFraction = 0.56f
                    val gaugeRadiusDp = minOf(stageW * 0.112f, stageH * 0.255f).coerceIn(29.dp, 36.dp)
                    val gaugeCenterXDp = stageW * gaugeCenterXFraction
                    val gaugeCenterYDp = stageH * gaugeCenterYFraction

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Horizontal zones (guaranteed zero overlap for 1..6 pumps!)
                        val tankLeft = w * 0.015f
                        val tankW = w * 0.185f
                        val tankTop = h * 0.14f
                        val tankH = h * 0.78f
                        val tankBottom = tankTop + tankH
                        val tankRight = tankLeft + tankW

                        val pumpZoneLeft = w * 0.265f
                        val pumpZoneRight = w * 0.745f
                        val pumpZoneW = pumpZoneRight - pumpZoneLeft

                        val suctionY = h * 0.84f
                        val dischargeY = h * 0.16f
                        val pumpMidY = (suctionY + dischargeY) * 0.50f

                        val gaugeCenterX = w * gaugeCenterXFraction
                        val gaugeCenterY = h * gaugeCenterYFraction
                        val gaugeR = gaugeRadiusDp.toPx()

                        val headerOuter = 9.dp.toPx()
                        val headerInner = 5.4.dp.toPx()
                        val riserOuter = 7.2.dp.toPx()
                        val riserInner = 4.2.dp.toPx()
                        val pipeWallColor = if (p.isDark) Color(0xFF475569) else Color(0xFF7B8CA6)
                        val flowPhasePx = flowOffsetDp.dp.toPx()
                        val dashPattern = floatArrayOf(10.dp.toPx(), 7.dp.toPx())
                        val activeDashEffect = PathEffect.dashPathEffect(dashPattern, -flowPhasePx)

                        val slotW = pumpZoneW / pumpCount.toFloat()
                        val pumpCentersX = List(pumpCount) { i ->
                            pumpZoneLeft + slotW * (i + 0.5f)
                        }
                        val firstPumpX = pumpCentersX.first()
                        val lastPumpX = pumpCentersX.last()
                        val dischargeEndX = w * 0.975f

                        val pumpR = when (pumpCount) {
                            1 -> 22.dp.toPx()
                            2 -> 19.5.dp.toPx()
                            3 -> 17.5.dp.toPx()
                            4 -> 15.dp.toPx()
                            else -> 12.8.dp.toPx()
                        }.coerceAtMost(slotW * 0.41f)

                        val effectiveRiserOuter = riserOuter.coerceAtMost(pumpR * 0.56f)
                        val effectiveRiserInner = riserInner.coerceAtMost(pumpR * 0.34f)

                        val runningFlags = pumps.map { it.failed != true && it.enabled != false && it.running == true }
                        val firstRunningIdx = runningFlags.indexOfFirst { it }
                        val lastRunningIdx = runningFlags.indexOfLast { it }

                        // ---- A. WATER STORAGE TANK + INLET PIPE + FLOAT SWITCH ELECTRODES ----
                        val fillPipeX = tankLeft + tankW * 0.28f
                        drawLine(
                            color = pipeWallColor,
                            start = Offset(fillPipeX, h * 0.03f),
                            end = Offset(fillPipeX, tankTop + 3.dp.toPx()),
                            strokeWidth = 5.6.dp.toPx(),
                            cap = StrokeCap.Butt,
                        )
                        drawLine(
                            color = Color(0xFF38BDF8).copy(alpha = 0.72f),
                            start = Offset(fillPipeX, h * 0.03f),
                            end = Offset(fillPipeX, tankTop + 3.dp.toPx()),
                            strokeWidth = 2.8.dp.toPx(),
                        )
                        drawLine(
                            color = pipeWallColor,
                            start = Offset(fillPipeX - 4.5.dp.toPx(), h * 0.03f),
                            end = Offset(fillPipeX + 4.5.dp.toPx(), h * 0.03f),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )

                        // Tank vessel recessed interior
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                if (p.isDark) {
                                    listOf(Color(0xFF121824), Color(0xFF1D273B), Color(0xFF101622))
                                } else {
                                    listOf(Color(0xFFDCE4F0), Color(0xFFF3F6FA), Color(0xFFD3DCEB))
                                },
                                startX = tankLeft,
                                endX = tankRight,
                            ),
                            topLeft = Offset(tankLeft, tankTop),
                            size = Size(tankW, tankH),
                            cornerRadius = CornerRadius(7.dp.toPx()),
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
                                    val amp = if (tankLevel < 0.22f) 1.5f else 2.5f
                                    val y = waterTop + sin(wavePhase + s * 0.55f) * amp
                                    lineTo(x, y)
                                }
                                lineTo(tankRight - 2.dp.toPx(), tankBottom - 2.dp.toPx())
                                close()
                            }
                            drawPath(
                                path = waterPath,
                                brush = Brush.verticalGradient(
                                    0f to tankWaterColor.copy(alpha = 0.82f),
                                    1f to tankWaterColor.copy(alpha = 0.34f),
                                    startY = waterTop,
                                    endY = tankBottom,
                                ),
                            )
                        }

                        // Dashed High (H) & Low (L) level reference lines inside tank
                        val highLineY = tankTop + tankH * 0.30f
                        val lowLineY = tankTop + tankH * 0.74f
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
                        drawLine(
                            color = if (snapshot.floaterHighConnected == true) p.ok.copy(alpha = 0.85f) else p.textMuted.copy(alpha = 0.45f),
                            start = Offset(tankLeft + 3.dp.toPx(), highLineY),
                            end = Offset(tankRight - 3.dp.toPx(), highLineY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect,
                        )
                        drawLine(
                            color = if (snapshot.floaterLowConnected == true) p.ok.copy(alpha = 0.85f) else p.fault.copy(alpha = 0.90f),
                            start = Offset(tankLeft + 3.dp.toPx(), lowLineY),
                            end = Offset(tankRight - 3.dp.toPx(), lowLineY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect,
                        )

                        // 3 Vertical Float Switch Electrode Rods (H, L, C)
                        val rodH_X = tankRight - tankW * 0.36f
                        val rodL_X = tankRight - tankW * 0.23f
                        val rodC_X = tankRight - tankW * 0.11f
                        val rodTopY = tankTop - 4.dp.toPx()
                        val rodColor = if (p.isDark) Color(0xFFB5C0D4) else Color(0xFF475569)

                        drawLine(rodColor.copy(alpha = 0.72f), Offset(rodC_X, rodTopY), Offset(rodC_X, tankBottom - 4.dp.toPx()), strokeWidth = 1.1.dp.toPx())
                        drawLine(rodColor.copy(alpha = 0.85f), Offset(rodL_X, rodTopY), Offset(rodL_X, lowLineY), strokeWidth = 1.2.dp.toPx())
                        val lowDotColor = when (snapshot.floaterLowConnected) {
                            true -> p.ok
                            false -> p.fault
                            null -> p.standby
                        }
                        if (lowWater) {
                            drawCircle(p.fault.copy(alpha = 0.35f * pulse), radius = 6.dp.toPx(), center = Offset(rodL_X, lowLineY))
                        }
                        drawCircle(lowDotColor, radius = 3.dp.toPx(), center = Offset(rodL_X, lowLineY))

                        drawLine(rodColor.copy(alpha = 0.85f), Offset(rodH_X, rodTopY), Offset(rodH_X, highLineY), strokeWidth = 1.2.dp.toPx())
                        val highDotColor = when (snapshot.floaterHighConnected) {
                            true -> p.ok
                            false -> p.warning
                            null -> p.standby
                        }
                        drawCircle(highDotColor, radius = 3.dp.toPx(), center = Offset(rodH_X, highLineY))

                        // Tank border outline
                        drawRoundRect(
                            color = if (lowWater) p.fault.copy(alpha = 0.85f) else p.panelBorder,
                            topLeft = Offset(tankLeft, tankTop),
                            size = Size(tankW, tankH),
                            cornerRadius = CornerRadius(7.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx()),
                        )

                        // ---- B. TRUE 2-PASS CONTINUOUS HYDRAULIC PIPE NETWORK (NO DEAD ENDS!) ----
                        // PASS 1: Draw ALL outer metallic pipe walls first so T-junctions & elbows merge cleanly.

                        // 1a. Bottom suction header outer wall: tank outlet -> clean 90-deg elbow at last pump
                        drawLine(
                            color = pipeWallColor,
                            start = Offset(tankRight - 1.dp.toPx(), suctionY),
                            end = Offset(lastPumpX, suctionY),
                            strokeWidth = headerOuter,
                            cap = StrokeCap.Round,
                        )
                        // 1b. Vertical suction & discharge riser outer walls for EVERY pump
                        pumpCentersX.forEach { cx ->
                            drawLine(
                                color = pipeWallColor,
                                start = Offset(cx, suctionY),
                                end = Offset(cx, pumpMidY + pumpR * 0.82f),
                                strokeWidth = effectiveRiserOuter,
                                cap = StrokeCap.Round,
                            )
                            drawLine(
                                color = pipeWallColor,
                                start = Offset(cx, pumpMidY - pumpR * 0.82f),
                                end = Offset(cx, dischargeY),
                                strokeWidth = effectiveRiserOuter,
                                cap = StrokeCap.Round,
                            )
                        }
                        // 1c. Top discharge collector outer wall: clean 90-deg elbow at Pump 1 -> system outlet
                        drawLine(
                            color = pipeWallColor,
                            start = Offset(firstPumpX, dischargeY),
                            end = Offset(dischargeEndX - 3.dp.toPx(), dischargeY),
                            strokeWidth = headerOuter,
                            cap = StrokeCap.Round,
                        )
                        // 1d. Pressure gauge tap stem outer wall connecting discharge header to circular gauge
                        drawLine(
                            color = pipeWallColor,
                            start = Offset(gaugeCenterX, dischargeY + headerOuter * 0.2f),
                            end = Offset(gaugeCenterX, gaugeCenterY - gaugeR),
                            strokeWidth = 4.2.dp.toPx(),
                            cap = StrokeCap.Butt,
                        )

                        // PASS 2: Draw ALL continuous BLUE hydraulic water cores ON TOP of Pass 1 walls!
                        // Every T-junction, elbow, and tank exit is now 100% open and hydraulically connected.

                        // 2a. Full static blue water channel across entire suction manifold (starting inside tank!)
                        drawLine(
                            color = staticPipeWaterColor,
                            start = Offset(tankRight - 3.5.dp.toPx(), suctionY),
                            end = Offset(lastPumpX, suctionY),
                            strokeWidth = headerInner,
                            cap = StrokeCap.Round,
                        )
                        // 2b. Full static blue water channel inside EVERY pump's suction & discharge risers
                        pumpCentersX.forEach { cx ->
                            drawLine(
                                color = staticPipeWaterColor,
                                start = Offset(cx, suctionY),
                                end = Offset(cx, pumpMidY + pumpR * 0.82f),
                                strokeWidth = effectiveRiserInner,
                                cap = StrokeCap.Round,
                            )
                            drawLine(
                                color = staticPipeWaterColor,
                                start = Offset(cx, pumpMidY - pumpR * 0.82f),
                                end = Offset(cx, dischargeY),
                                strokeWidth = effectiveRiserInner,
                                cap = StrokeCap.Round,
                            )
                        }
                        // 2c. Full static blue water channel across entire top discharge manifold & gauge stem
                        drawLine(
                            color = staticPipeWaterColor,
                            start = Offset(firstPumpX, dischargeY),
                            end = Offset(dischargeEndX - 3.dp.toPx(), dischargeY),
                            strokeWidth = headerInner,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = if (isFlowing) activePipeWaterBase else staticPipeWaterColor,
                            start = Offset(gaugeCenterX, dischargeY),
                            end = Offset(gaugeCenterX, gaugeCenterY - gaugeR),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )

                        // PASS 2B: Active Pressurized Blue Water Stream along currently flowing path!
                        if (isFlowing && firstRunningIdx >= 0 && lastRunningIdx >= 0) {
                            val suctionActiveEndX = pumpCentersX[lastRunningIdx]
                            val dischargeActiveStartX = pumpCentersX[firstRunningIdx]

                            // Active bottom suction header: Tank -> furthest running pump
                            drawLine(
                                color = activePipeWaterBase,
                                start = Offset(tankRight - 3.5.dp.toPx(), suctionY),
                                end = Offset(suctionActiveEndX, suctionY),
                                strokeWidth = headerInner,
                                cap = StrokeCap.Round,
                            )
                            drawLine(
                                color = activeFlowStreamColor,
                                start = Offset(tankRight - 1.dp.toPx(), suctionY),
                                end = Offset(suctionActiveEndX, suctionY),
                                strokeWidth = headerInner * 0.60f,
                                cap = StrokeCap.Round,
                                pathEffect = activeDashEffect,
                            )

                            // Active pump risers (upward continuous blue flow through every running pump!)
                            pumpCentersX.forEachIndexed { idx, cx ->
                                if (runningFlags[idx]) {
                                    // Suction riser: bottom header -> pump inlet
                                    drawLine(
                                        color = activePipeWaterBase,
                                        start = Offset(cx, suctionY),
                                        end = Offset(cx, pumpMidY + pumpR * 0.82f),
                                        strokeWidth = effectiveRiserInner,
                                        cap = StrokeCap.Round,
                                    )
                                    drawLine(
                                        color = activeFlowStreamColor,
                                        start = Offset(cx, suctionY),
                                        end = Offset(cx, pumpMidY + pumpR * 0.82f),
                                        strokeWidth = effectiveRiserInner * 0.62f,
                                        cap = StrokeCap.Round,
                                        pathEffect = activeDashEffect,
                                    )
                                    // Discharge riser: pump outlet -> top collector
                                    drawLine(
                                        color = activePipeWaterBase,
                                        start = Offset(cx, pumpMidY - pumpR * 0.82f),
                                        end = Offset(cx, dischargeY),
                                        strokeWidth = effectiveRiserInner,
                                        cap = StrokeCap.Round,
                                    )
                                    drawLine(
                                        color = activeFlowStreamColor,
                                        start = Offset(cx, pumpMidY - pumpR * 0.82f),
                                        end = Offset(cx, dischargeY),
                                        strokeWidth = effectiveRiserInner * 0.62f,
                                        cap = StrokeCap.Round,
                                        pathEffect = activeDashEffect,
                                    )
                                }
                            }

                            // Active top discharge collector: earliest running pump -> system outlet
                            drawLine(
                                color = activePipeWaterBase,
                                start = Offset(dischargeActiveStartX, dischargeY),
                                end = Offset(dischargeEndX - 3.dp.toPx(), dischargeY),
                                strokeWidth = headerInner,
                                cap = StrokeCap.Round,
                            )
                            drawLine(
                                color = activeFlowStreamColor,
                                start = Offset(dischargeActiveStartX, dischargeY),
                                end = Offset(dischargeEndX - 4.dp.toPx(), dischargeY),
                                strokeWidth = headerInner * 0.60f,
                                cap = StrokeCap.Round,
                                pathEffect = activeDashEffect,
                            )
                        }

                        // Clean Inlet Isolation Valve symbol between Tank and Pump 1
                        val valveX = tankRight + (pumpZoneLeft - tankRight) * 0.48f
                        val valveR = 4.6.dp.toPx()
                        val valveTri = Path().apply {
                            moveTo(valveX - valveR, suctionY - valveR)
                            lineTo(valveX + valveR, suctionY + valveR)
                            lineTo(valveX + valveR, suctionY - valveR)
                            lineTo(valveX - valveR, suctionY + valveR)
                            close()
                        }
                        drawPath(valveTri, color = p.gold.copy(alpha = 0.88f))
                        drawLine(
                            color = p.gold,
                            start = Offset(valveX, suctionY - valveR),
                            end = Offset(valveX, suctionY - valveR - 5.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx(),
                        )
                        drawLine(
                            color = p.gold,
                            start = Offset(valveX - 2.dp.toPx(), suctionY - valveR - 5.dp.toPx()),
                            end = Offset(valveX + 6.5.dp.toPx(), suctionY - valveR - 5.dp.toPx()),
                            strokeWidth = 1.8.dp.toPx(),
                            cap = StrokeCap.Round,
                        )

                        // Discharge outlet directional chevron arrow (hydraulic blue when flowing)
                        val arrowX = dischargeEndX - 1.dp.toPx()
                        val arrowPath = Path().apply {
                            moveTo(arrowX - 5.5.dp.toPx(), dischargeY - 4.5.dp.toPx())
                            lineTo(arrowX + 1.dp.toPx(), dischargeY)
                            lineTo(arrowX - 5.5.dp.toPx(), dischargeY + 4.5.dp.toPx())
                        }
                        drawPath(
                            path = arrowPath,
                            color = if (isFlowing) Color(0xFF38BDF8) else p.textMuted,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )

                        // ---- C. PARALLEL PUMPS (1..6) DRAWN CLEANLY OVER PIPE CONNECTIONS ----
                        val inactiveGray = if (p.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

                        pumps.forEachIndexed { idx, pump ->
                            val cx = pumpCentersX[idx]
                            val isFault = pump.failed == true
                            val isDisabled = pump.enabled == false && !isFault
                            val isRunning = runningFlags[idx]
                            val isReady = !isFault && !isDisabled && !isRunning && pump.enabled == true
                            val isDedicatedVfd = mode == 1 && (idx + 1) <= driveCount

                            val casingColor = when {
                                isFault -> p.fault
                                isDisabled -> p.warning
                                isRunning -> p.ok
                                isReady -> p.cyan
                                else -> inactiveGray
                            }
                            val ledColor = when {
                                isFault -> p.fault
                                isDisabled -> p.warning
                                isRunning -> p.ok
                                isReady -> p.cyan
                                else -> p.standby
                            }

                            // Subtle steel pipe collar flanges where risers join the volute casing
                            val collarHalfW = effectiveRiserOuter * 0.72f
                            drawLine(
                                color = pipeWallColor,
                                start = Offset(cx - collarHalfW, pumpMidY - pumpR - 1.5.dp.toPx()),
                                end = Offset(cx + collarHalfW, pumpMidY - pumpR - 1.5.dp.toPx()),
                                strokeWidth = 1.8.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                            drawLine(
                                color = pipeWallColor,
                                start = Offset(cx - collarHalfW, pumpMidY + pumpR + 1.5.dp.toPx()),
                                end = Offset(cx + collarHalfW, pumpMidY + pumpR + 1.5.dp.toPx()),
                                strokeWidth = 1.8.dp.toPx(),
                                cap = StrokeCap.Round,
                            )

                            val center = Offset(cx, pumpMidY)

                            // Ambient active/fault glow halo
                            if (isRunning || isFault) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        0f to casingColor.copy(alpha = 0.34f * pulse),
                                        1f to Color.Transparent,
                                        center = center,
                                        radius = pumpR * 1.52f,
                                    ),
                                    radius = pumpR * 1.52f,
                                    center = center,
                                )
                            }

                            // Dedicated VFD gold outer ring in Mode 1
                            if (isDedicatedVfd) {
                                drawCircle(
                                    color = p.gold.copy(alpha = if (isRunning) 0.90f else 0.45f),
                                    radius = pumpR + 2.8.dp.toPx(),
                                    center = center,
                                    style = Stroke(width = 1.1.dp.toPx()),
                                )
                            }

                            // Volute casing fill & outer ring
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0f to Color(0xFF141C2B),
                                    0.74f to Color(0xFF0E1522),
                                    1f to casingColor.copy(alpha = 0.24f),
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
                                style = Stroke(width = (pumpR * 0.14f).coerceIn(1.6.dp.toPx(), 2.4.dp.toPx())),
                            )

                            // 5-blade curved centrifugal impeller
                            val rot = if (isRunning) impellerAngle else 0f
                            rotate(degrees = rot, pivot = center) {
                                val bladeStroke = (pumpR * 0.11f).coerceIn(1.2.dp.toPx(), 2.dp.toPx())
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
                                            color = if (isRunning) Color(0xFF7DD3FC) else casingColor.copy(alpha = 0.78f),
                                            style = Stroke(width = bladeStroke, cap = StrokeCap.Round),
                                        )
                                    }
                                }
                            }
                            // Shaft hub
                            drawCircle(
                                color = if (isRunning) p.gold else casingColor,
                                radius = (pumpR * 0.20f).coerceAtLeast(2.1.dp.toPx()),
                                center = center,
                            )

                            // Crisp top-right status LED beacon
                            val ledCenter = Offset(cx + pumpR * 0.72f, pumpMidY - pumpR * 0.72f)
                            val ledR = (pumpR * 0.22f).coerceIn(2.8.dp.toPx(), 4.4.dp.toPx())
                            if (isRunning || isFault) {
                                drawCircle(ledColor.copy(alpha = 0.36f * pulse), radius = ledR * 1.65f, center = ledCenter)
                            }
                            drawCircle(p.terminalBg, radius = ledR + 1.dp.toPx(), center = ledCenter)
                            drawCircle(ledColor, radius = ledR, center = ledCenter)
                        }

                        // ---- D. COMPACT LUXURY CIRCULAR PRESSURE DIAL (Bar & SP inside!) ----
                        drawCircle(
                            color = Color(0xFF38BDF8).copy(alpha = 0.85f),
                            radius = 2.8.dp.toPx(),
                            center = Offset(gaugeCenterX, dischargeY),
                        )

                        val dialCenter = Offset(gaugeCenterX, gaugeCenterY)
                        val gaugeColor = when {
                            hasActiveAlarms -> p.fault
                            snapshot.pressureBar != null && snapshot.pressureBar <= 0.5 -> p.warning
                            else -> Color(0xFF38BDF8)
                        }

                        // Subtle outer ambient bezel glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                0.72f to Color.Transparent,
                                1f to gaugeColor.copy(alpha = 0.18f),
                                center = dialCenter,
                                radius = gaugeR * 1.18f,
                            ),
                            radius = gaugeR * 1.18f,
                            center = dialCenter,
                        )
                        // Deep recessed sapphire-obsidian dial face
                        drawCircle(
                            brush = Brush.radialGradient(
                                0f to Color(0xFF151E30),
                                0.82f to Color(0xFF0C121E),
                                1f to Color(0xFF080C14),
                                center = dialCenter,
                                radius = gaugeR,
                            ),
                            radius = gaugeR,
                            center = dialCenter,
                        )
                        // Brushed champagne-gold outer ring bezel
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    p.gold.copy(alpha = 0.45f),
                                    p.goldBright.copy(alpha = 0.92f),
                                    p.gold.copy(alpha = 0.55f),
                                    p.gold.copy(alpha = 0.45f),
                                ),
                                center = dialCenter,
                            ),
                            radius = gaugeR,
                            center = dialCenter,
                            style = Stroke(width = 1.6.dp.toPx()),
                        )

                        // Delicate precision tick marks along 240-deg arc
                        val dialStart = 150f
                        val dialSweep = 240f
                        for (t in 0..8) {
                            val tickRad = (dialStart + dialSweep * (t / 8f)) * PI / 180.0
                            val rOuter = gaugeR - 2.6.dp.toPx()
                            val rInner = gaugeR - if (t % 2 == 0) 5.2.dp.toPx() else 4.0.dp.toPx()
                            drawLine(
                                color = Color.White.copy(alpha = if (t % 2 == 0) 0.28f else 0.14f),
                                start = Offset(
                                    dialCenter.x + (rInner * cos(tickRad)).toFloat(),
                                    dialCenter.y + (rInner * sin(tickRad)).toFloat(),
                                ),
                                end = Offset(
                                    dialCenter.x + (rOuter * cos(tickRad)).toFloat(),
                                    dialCenter.y + (rOuter * sin(tickRad)).toFloat(),
                                ),
                                strokeWidth = 1.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }

                        // Clean 240-deg circular progress arc track (leaves center completely open for numbers!)
                        val arcInset = 5.4.dp.toPx()
                        val arcTopLeft = Offset(dialCenter.x - gaugeR + arcInset, dialCenter.y - gaugeR + arcInset)
                        val arcSize = Size((gaugeR - arcInset) * 2f, (gaugeR - arcInset) * 2f)
                        drawArc(
                            color = Color(0xFF222E44),
                            startAngle = dialStart,
                            sweepAngle = dialSweep,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
                        )

                        if (snapshot.pressureBar != null && pressureFrac > 0.005f) {
                            // Glowing pressure sweep
                            drawArc(
                                color = gaugeColor.copy(alpha = 0.30f),
                                startAngle = dialStart,
                                sweepAngle = dialSweep * pressureFrac,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = 6.2.dp.toPx(), cap = StrokeCap.Round),
                            )
                            drawArc(
                                color = gaugeColor,
                                startAngle = dialStart,
                                sweepAngle = dialSweep * pressureFrac,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
                            )
                            // Luminous live indicator bead at arc tip
                            val tipRad = (dialStart + dialSweep * pressureFrac) * PI / 180.0
                            val tipR = gaugeR - arcInset
                            val tipPos = Offset(
                                dialCenter.x + (tipR * cos(tipRad)).toFloat(),
                                dialCenter.y + (tipR * sin(tipRad)).toFloat(),
                            )
                            drawCircle(Color.White, radius = 2.2.dp.toPx(), center = tipPos)
                        }

                        // Gold Setpoint target marker notch on outer arc
                        snapshot.pressureSetpointBar?.takeIf { it > 0.0 }?.let { sp ->
                            val spFrac = (sp / maxBar).toFloat().coerceIn(0f, 1f)
                            val spRad = (dialStart + dialSweep * spFrac) * PI / 180.0
                            drawLine(
                                color = p.goldBright,
                                start = Offset(
                                    dialCenter.x + ((gaugeR - 7.8.dp.toPx()) * cos(spRad)).toFloat(),
                                    dialCenter.y + ((gaugeR - 7.8.dp.toPx()) * sin(spRad)).toFloat(),
                                ),
                                end = Offset(
                                    dialCenter.x + ((gaugeR - 2.dp.toPx()) * cos(spRad)).toFloat(),
                                    dialCenter.y + ((gaugeR - 2.dp.toPx()) * sin(spRad)).toFloat(),
                                ),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }
                    }

                    // Crisp Digital Bar + SP Overlay Centered Directly INSIDE the Pressure Dial Circle!
                    val baseDensity = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(density = baseDensity.density, fontScale = 1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = gaugeCenterXDp - gaugeRadiusDp,
                                    y = gaugeCenterYDp - gaugeRadiusDp,
                                )
                                .size(gaugeRadiusDp * 2),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                Text(
                                    text = snapshot.pressureBar?.let { String.format(Locale.US, "%.2f", it) } ?: "—",
                                    style = MonoStyle.copy(
                                        fontSize = 12.5.sp,
                                        lineHeight = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = Color(0xFFF8FAFC),
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    text = "Bar",
                                    style = MonoStyle.copy(
                                        fontSize = 7.5.sp,
                                        lineHeight = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = Color(0xFF38BDF8),
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    text = snapshot.pressureSetpointBar?.let {
                                        String.format(Locale.US, "SP %.1f", it)
                                    } ?: "SP —",
                                    style = MonoStyle.copy(
                                        fontSize = 7.sp,
                                        lineHeight = 8.5.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = p.gold,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                // 2. Tightly Aligned Tank (H/L) & Pump Tags (P1..PN) Directly Below Tank & Suction Pipe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Left (22%): Tank + Floater H/L directly under tank vessel
                    Column(
                        modifier = Modifier.weight(0.22f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(
                            text = "Tank",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color(0xFFF1F5F9),
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
                            style = MonoStyle.copy(
                                fontSize = 8.5.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = tankWaterColor,
                            textAlign = TextAlign.Center,
                        )
                    }

                    // Center (53%): Dynamic Pump Tags (P1..PN) + Continuous Drive Frequency Directly Below!
                    Column(
                        modifier = Modifier.weight(0.53f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Top,
                        ) {
                            pumps.forEach { pump ->
                                val isFault = pump.failed == true
                                val isDisabled = pump.enabled == false && !isFault
                                val isRunning = !isFault && !isDisabled && pump.running == true
                                val isReady = !isFault && !isDisabled && !isRunning && pump.enabled == true
                                val tagColor = when {
                                    isFault -> p.fault
                                    isDisabled -> p.warning
                                    isRunning -> p.ok
                                    else -> Color(0xFFF1F5F9)
                                }
                                val subTag = when {
                                    isFault -> "ERR"
                                    isDisabled -> "S"
                                    isRunning -> "RUN"
                                    isReady -> "RDY"
                                    else -> "RDY"
                                }
                                val subColor = when {
                                    isFault -> p.fault
                                    isDisabled -> p.warning
                                    isRunning -> p.ok
                                    else -> Color(0xFF38BDF8)
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(0.dp),
                                ) {
                                    Text(
                                        text = "P${pump.number}",
                                        style = MonoStyle.copy(
                                            fontSize = if (pumpCount >= 5) 9.sp else 10.sp,
                                            lineHeight = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = tagColor,
                                        textAlign = TextAlign.Center,
                                    )
                                    Text(
                                        text = subTag,
                                        style = MonoStyle.copy(
                                            fontSize = if (pumpCount >= 5) 7.5.sp else 8.5.sp,
                                            lineHeight = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = subColor,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }

                        // Continuous Live Drive Frequency Pill Directly Under the Pumps (as annotated!)
                        val driveLabel = when {
                            driveCount == 0 -> "Fixed-Speed (DOL)"
                            driveCount >= 2 && snapshot.drive1FrequencyHz != null && snapshot.drive2FrequencyHz != null ->
                                String.format(
                                    Locale.US,
                                    "D1: %.1f Hz  •  D2: %.1f Hz",
                                    snapshot.drive1FrequencyHz,
                                    snapshot.drive2FrequencyHz,
                                )
                            snapshot.drive1FrequencyHz != null ->
                                String.format(Locale.US, "Drive: %.1f Hz", snapshot.drive1FrequencyHz)
                            else -> "Drive: — Hz"
                        }
                        val driveActive = driveCount > 0 && (snapshot.drStatus == true || (activeDriveFreqHz ?: 0.0) > 0.1)
                        val driveAccent = when {
                            snapshot.driveFailed == true -> p.fault
                            snapshot.driveEnabled == false -> p.warning
                            driveActive -> p.gold
                            else -> Color(0xFF94A3B8)
                        }

                        Surface(
                            color = Color(0xFF141B29),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.8.dp, driveAccent.copy(alpha = 0.48f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                                ) {
                                    if (driveActive) {
                                        LivePulseDot(color = driveAccent, modifier = Modifier.size(7.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(5.5.dp)
                                                .clip(CircleShape)
                                                .background(driveAccent),
                                        )
                                    }
                                    Text(
                                        text = driveLabel,
                                        style = MonoStyle.copy(
                                            fontSize = 9.5.sp,
                                            lineHeight = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = driveAccent,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                if (driveCount > 0) {
                                    // Smooth continuous frequency progress micro-bar (0..50 Hz)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color(0xFF1E293B)),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(animatedDriveFrac)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFF38BDF8), driveAccent),
                                                    ),
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Right (25%): Subtle pressure caption under circular gauge (values are inside the circle!)
                    Column(
                        modifier = Modifier.weight(0.25f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(
                            text = "Pressure",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                lineHeight = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = if (isFlowing) "Active" else "Idle",
                            style = MonoStyle.copy(
                                fontSize = 8.sp,
                                lineHeight = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (isFlowing) Color(0xFF38BDF8) else Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // 3. Complete Color-Coded Status Legend Explicitly Explaining RUN, RDY, S, and ERR!
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SchematicLegendDot(color = p.ok, code = "RUN", label = "Running")
                SchematicLegendDot(color = p.cyan, code = "RDY", label = "Ready (Standby)")
                SchematicLegendDot(color = p.warning, code = "S", label = "Service (Off)")
                SchematicLegendDot(color = p.fault, code = "ERR", label = "Fault")
            }
        }
    }
}

@Composable
private fun SchematicLegendDot(color: Color, code: String, label: String) {
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
            text = code,
            style = MonoStyle.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
            color = color,
        )
        Text(
            text = "— $label",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = p.textSecondary,
        )
    }
}
