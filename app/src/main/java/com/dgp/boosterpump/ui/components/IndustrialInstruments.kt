package com.dgp.boosterpump.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.controller.PhaseVoltages
import com.dgp.boosterpump.session.PressureSample
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.ui.theme.MonoStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 240-degree industrial SCADA circular pressure gauge with scale ticks, multi-zone color
 * arc, smooth animated sweep, glow halo, and high-contrast central digital readout.
 */
@Composable
fun PressureArcGauge(
    pressureBar: Double?,
    setpointBar: Double? = null,
    maxBar: Double = 10.0,
    drActive: Boolean? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalScadaPalette.current
    val effectiveMax = maxOf(maxBar, ((setpointBar ?: 0.0) + 2.0), ((pressureBar ?: 0.0) + 1.5))
    val targetFraction = ((pressureBar ?: 0.0) / effectiveMax).toFloat().coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "pressure_sweep",
    )

    val activeColor = when {
        pressureBar == null -> palette.standby
        pressureBar >= effectiveMax * 0.78 -> palette.fault
        pressureBar >= effectiveMax * 0.65 -> palette.warning
        pressureBar <= 0.5 -> palette.warning
        else -> palette.cyan
    }

    val gaugePulse = rememberInfiniteTransition(label = "gauge_pulse")
    val tipGlowPulse by gaugePulse.animateFloat(
        initialValue = 7.5f,
        targetValue = 12.5f,
        animationSpec = infiniteRepeatable(tween(1150, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tip_glow_pulse",
    )
    val haloAlpha by gaugePulse.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(tween(1150, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "halo_alpha",
    )

    Box(
        modifier = modifier.size(166.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 14.dp.toPx()
            val inset = strokePx + 10.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = arcSize.width / 2f
            val startAngle = 150f
            val totalSweep = 240f

            // Outer subtle bezel ring
            drawCircle(
                color = palette.gold.copy(alpha = 0.30f),
                radius = radius + strokePx * 0.88f,
                center = center,
                style = Stroke(width = 1.2.dp.toPx()),
            )

            // Scale tick marks
            val tickCount = 20
            for (i in 0..tickCount) {
                val deg = startAngle + (totalSweep * i / tickCount)
                val rad = deg * PI / 180.0
                val isMajor = i % 2 == 0
                val rOuter = radius + strokePx * 0.65f
                val rInner = radius + strokePx * (if (isMajor) 0.15f else 0.38f)
                val tickColor = when {
                    i >= 16 -> palette.fault.copy(alpha = 0.78f)
                    i >= 13 -> palette.warning.copy(alpha = 0.78f)
                    isMajor -> palette.gold.copy(alpha = 0.65f)
                    else -> palette.textSecondary.copy(alpha = 0.35f)
                }
                drawLine(
                    color = tickColor,
                    start = Offset(
                        center.x + (rInner * cos(rad)).toFloat(),
                        center.y + (rInner * sin(rad)).toFloat(),
                    ),
                    end = Offset(
                        center.x + (rOuter * cos(rad)).toFloat(),
                        center.y + (rOuter * sin(rad)).toFloat(),
                    ),
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // Track arc
            drawArc(
                color = palette.panelElevated,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // Active sweep + breathing glow halo
            if (pressureBar != null && animatedFraction > 0.002f) {
                val sweep = totalSweep * animatedFraction
                drawArc(
                    color = activeColor.copy(alpha = haloAlpha),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx + 9.dp.toPx(), cap = StrokeCap.Round),
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to palette.ok,
                        0.5f to palette.cyan,
                        0.78f to palette.warning,
                        1.0f to palette.fault,
                        center = center,
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )

                // Breathing luminous tip indicator dot
                val tipRad = (startAngle + sweep) * PI / 180.0
                val tipOffset = Offset(
                    center.x + (radius * cos(tipRad)).toFloat(),
                    center.y + (radius * sin(tipRad)).toFloat(),
                )
                drawCircle(color = activeColor.copy(alpha = 0.38f), radius = tipGlowPulse.dp.toPx(), center = tipOffset)
                drawCircle(color = Color.White, radius = 4.2.dp.toPx(), center = tipOffset)
            }

            // Brushed champagne-gold setpoint marker notch on the arc
            if (setpointBar != null && setpointBar > 0.0) {
                val spFraction = (setpointBar / effectiveMax).toFloat().coerceIn(0f, 1f)
                val spRad = (startAngle + totalSweep * spFraction) * PI / 180.0
                val rIn = radius - strokePx * 0.65f
                val rOut = radius + strokePx * 0.72f
                drawLine(
                    color = palette.gold,
                    start = Offset(center.x + (rIn * cos(spRad)).toFloat(), center.y + (rIn * sin(spRad)).toFloat()),
                    end = Offset(center.x + (rOut * cos(spRad)).toFloat(), center.y + (rOut * sin(spRad)).toFloat()),
                    strokeWidth = 2.6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            LtrText(
                text = "System Pressure",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = palette.textSecondary,
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = pressureBar?.let { "%.2f".format(it) } ?: "—",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp, lineHeight = 32.sp),
                    color = palette.textPrimary,
                )
            }
            LtrText(
                text = if (setpointBar != null) "SP: %.2f Bar".format(setpointBar) else "Bar",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = if (setpointBar != null) palette.gold else activeColor,
            )
            val drText = when (drActive) {
                true -> "Drive: ON"
                false -> "Drive: OFF"
                null -> "Drive: —"
            }
            LtrText(
                text = drText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (drActive == true) palette.ok else palette.textSecondary,
            )
        }
    }
}

/**
 * Animated industrial water storage tank cutaway driven by documented Floater L (Bit 32)
 * and Floater H (Bit 33) states.
 */
@Composable
fun WaterTankIndicator(
    floaterLow: Boolean?,
    floaterHigh: Boolean?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalScadaPalette.current
    val targetLevel = when {
        floaterLow == true && floaterHigh == true -> 0.84f
        floaterLow == true && floaterHigh == false -> 0.46f
        floaterLow == false && floaterHigh == false -> 0.12f
        floaterLow == false && floaterHigh == true -> 0.55f // sensor anomaly
        else -> 0.0f
    }
    val level by animateFloatAsState(
        targetValue = targetLevel,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "tank_level",
    )

    val waveTransition = rememberInfiniteTransition(label = "tank_wave")
    val wavePhase by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "wave_phase",
    )

    val (waterColor, statusLabel, statusTone) = when {
        floaterLow == true && floaterHigh == true -> Triple(palette.cyan, "Full", BadgeTone.OK)
        floaterLow == true && floaterHigh == false -> Triple(palette.warning, "Mid Level", BadgeTone.WARNING)
        floaterLow == false && floaterHigh == false -> Triple(palette.fault, "Low Water!", BadgeTone.FAULT)
        floaterLow == false && floaterHigh == true -> Triple(palette.fault, "Floater Fault", BadgeTone.FAULT)
        else -> Triple(palette.standby, "Unknown", BadgeTone.NEUTRAL)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LtrText("Water Tank", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)

        Box(
            modifier = Modifier
                .size(width = 86.dp, height = 114.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.panelElevated)
                .border(1.5.dp, palette.panelBorder, RoundedCornerShape(12.dp)),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (level > 0.01f) {
                    val waterTop = h * (1f - level)
                    val path = Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, waterTop)
                        val steps = 24
                        for (i in 0..steps) {
                            val x = w * i / steps
                            val amp = if (level < 0.2f) 2.5f else 4f
                            val y = waterTop + sin(wavePhase + (i * 0.45f)) * amp
                            lineTo(x, y)
                        }
                        lineTo(w, h)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            0f to waterColor.copy(alpha = 0.78f),
                            1f to waterColor.copy(alpha = 0.30f),
                            startY = waterTop,
                            endY = h,
                        ),
                    )
                }

                // High-level sensor line (22% from top) & Low-level sensor line (72% from top)
                val highY = h * 0.24f
                val lowY = h * 0.72f
                drawLine(
                    color = if (floaterHigh == true) palette.ok else palette.textSecondary.copy(alpha = 0.45f),
                    start = Offset(6f, highY),
                    end = Offset(w - 6f, highY),
                    strokeWidth = 1.5.dp.toPx(),
                )
                drawLine(
                    color = if (floaterLow == true) palette.ok else palette.fault.copy(alpha = 0.75f),
                    start = Offset(6f, lowY),
                    end = Offset(w - 6f, lowY),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }

            // H / L sensor indicators
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("H", style = MonoStyle.copy(fontSize = 10.sp), color = palette.textPrimary)
                    PlcLedBead(active = floaterHigh, size = 7.dp, activeColor = palette.ok)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("L", style = MonoStyle.copy(fontSize = 10.sp), color = palette.textPrimary)
                    PlcLedBead(active = floaterLow, size = 7.dp, activeColor = palette.ok, offColor = palette.fault)
                }
            }
        }

        StatusBadge(text = statusLabel, tone = statusTone)
    }
}

/**
 * Real-time SCADA pressure sparkline chart displaying the rolling session buffer with
 * horizontal gridlines, gradient area fill, and live pulsing point.
 */
@Composable
fun PressureTrendChart(
    samples: List<PressureSample>,
    modifier: Modifier = Modifier,
) {
    val palette = LocalScadaPalette.current
    val pulseTransition = rememberInfiniteTransition(label = "chart_pulse")
    val pulseRadius by pulseTransition.animateFloat(
        initialValue = 4f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse_r",
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (samples.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.panelElevated),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "در حال جمع‌آوری نمونه‌های فشار برای ترسیم نمودار زنده…",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }
            return
        }

        val values = samples.map { it.pressureBar }
        val minVal = values.min()
        val maxVal = values.max()
        val floor = (minVal - 0.5).coerceAtLeast(0.0)
        val ceil = (maxVal + 0.5).coerceAtLeast(floor + 1.0)
        val range = (ceil - floor).coerceAtLeast(0.5)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.panelElevated)
                .border(1.dp, palette.panelBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Horizontal reference gridlines
                for (i in 0..2) {
                    val y = h * i / 2f
                    drawLine(
                        color = palette.panelBorder.copy(alpha = 0.65f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                val pts = values.mapIndexed { idx, v ->
                    val x = if (values.size == 1) w else w * idx / (values.size - 1).toFloat()
                    val y = h - ((v - floor) / range).toFloat().coerceIn(0f, 1f) * h
                    Offset(x, y)
                }

                val fillPath = Path().apply {
                    moveTo(pts.first().x, h)
                    pts.forEach { lineTo(it.x, it.y) }
                    lineTo(pts.last().x, h)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        0f to palette.cyan.copy(alpha = 0.32f),
                        1f to Color.Transparent,
                    ),
                )

                val linePath = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = linePath,
                    color = palette.cyan,
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                val last = pts.last()
                drawCircle(color = palette.cyan.copy(alpha = 0.28f), radius = pulseRadius.dp.toPx(), center = last)
                drawCircle(color = palette.cyan, radius = 3.5.dp.toPx(), center = last)
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Min: %.2f Bar".format(minVal),
                    style = MonoStyle.copy(fontSize = 11.sp),
                    color = palette.textSecondary,
                )
                Text(
                    "${samples.size} live samples",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.cyan,
                )
                Text(
                    "Max: %.2f Bar".format(maxVal),
                    style = MonoStyle.copy(fontSize = 11.sp),
                    color = palette.textSecondary,
                )
            }
        }
    }
}

/**
 * Custom-drawn industrial centrifugal booster-pump emblem featuring a recognizable volute casing,
 * tangential top discharge flange, mounting pedestal, 5-blade curved turbine, and a clear state
 * medallion distinguishing Running (emerald), Ready (cyan), Disabled/Off 'S' (amber), and Fault (crimson).
 */
@Composable
fun AnimatedImpellerIcon(
    running: Boolean?,
    failed: Boolean?,
    disabled: Boolean = false,
    ready: Boolean = false,
    size: Dp = 40.dp,
) {
    val palette = LocalScadaPalette.current
    val isRunning = running == true && failed != true && !disabled
    val isFault = failed == true
    val isDisabled = disabled && !isFault
    val isReady = ready && !isRunning && !isDisabled && !isFault

    val infinite = rememberInfiniteTransition(label = "pump_emblem")
    val angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(950, easing = LinearEasing)),
        label = "pump_rotor_rot",
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(1050, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pump_glow_pulse",
    )

    val inactiveGray = if (palette.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val casingColor = when {
        isFault -> palette.fault
        isDisabled -> inactiveGray
        isRunning -> palette.ok
        isReady -> palette.cyan.copy(alpha = 0.85f)
        else -> palette.standby
    }
    val rotorColor = when {
        isFault -> palette.fault
        isDisabled -> inactiveGray
        isRunning -> palette.cyan
        isReady -> palette.cyan.copy(alpha = 0.78f)
        else -> palette.standby.copy(alpha = 0.75f)
    }
    val rotation = if (isRunning) angle else 0f

    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = this.size.minDimension

        // Center volute slightly lower-left so discharge flange (top-right) & base foot (bottom) sit cleanly in frame
        val voluteCenter = Offset(w * 0.47f, h * 0.54f)
        val voluteR = s * 0.34f
        val strokeThin = (s * 0.042f).coerceAtLeast(1.2.dp.toPx())
        val strokeMed = (s * 0.056f).coerceAtLeast(1.6.dp.toPx())

        // Ambient breathing halo for active pumping or fault
        if (isRunning || isFault) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to casingColor.copy(alpha = glowPulse),
                    1f to Color.Transparent,
                    center = voluteCenter,
                    radius = voluteR * 1.48f,
                ),
                radius = voluteR * 1.48f,
                center = voluteCenter,
            )
        }

        // 1. Industrial mounting base pedestal beneath volute
        val footY = voluteCenter.y + voluteR * 0.88f
        val footH = s * 0.12f
        val footW = voluteR * 1.56f
        val footLeft = voluteCenter.x - footW / 2f
        drawRoundRect(
            color = casingColor.copy(alpha = 0.36f),
            topLeft = Offset(footLeft, footY),
            size = Size(footW, footH),
            cornerRadius = CornerRadius(s * 0.04f),
        )
        drawRoundRect(
            color = casingColor.copy(alpha = 0.90f),
            topLeft = Offset(footLeft, footY),
            size = Size(footW, footH),
            cornerRadius = CornerRadius(s * 0.04f),
            style = Stroke(width = strokeThin * 0.85f),
        )

        // 2. Tangential top-right discharge pipe & bolted flange
        val pipeW = voluteR * 0.50f
        val pipeLeft = voluteCenter.x + voluteR * 0.20f
        val pipeTop = voluteCenter.y - voluteR * 1.24f
        val pipeH = voluteR * 0.82f
        drawRoundRect(
            color = if (isRunning) palette.cyan.copy(alpha = 0.28f) else casingColor.copy(alpha = 0.20f),
            topLeft = Offset(pipeLeft, pipeTop),
            size = Size(pipeW, pipeH),
            cornerRadius = CornerRadius(s * 0.04f),
        )
        drawRoundRect(
            color = casingColor.copy(alpha = 0.92f),
            topLeft = Offset(pipeLeft, pipeTop),
            size = Size(pipeW, pipeH),
            cornerRadius = CornerRadius(s * 0.04f),
            style = Stroke(width = strokeThin),
        )
        // Top discharge flange lip
        val flangeOver = s * 0.045f
        drawLine(
            color = if (isRunning) palette.cyan else casingColor,
            start = Offset(pipeLeft - flangeOver, pipeTop),
            end = Offset(pipeLeft + pipeW + flangeOver, pipeTop),
            strokeWidth = strokeMed,
            cap = StrokeCap.Round,
        )

        // 3. Circular volute pump body with metallic radial shading
        drawCircle(
            brush = Brush.radialGradient(
                0f to palette.panel,
                0.78f to palette.panelElevated,
                1f to casingColor.copy(alpha = 0.24f),
                center = voluteCenter,
                radius = voluteR,
            ),
            radius = voluteR,
            center = voluteCenter,
        )
        drawCircle(
            color = casingColor,
            radius = voluteR,
            center = voluteCenter,
            style = Stroke(width = strokeMed),
        )

        // 4. 5-blade swept centrifugal impeller (always clearly visible in both ON and OFF states)
        rotate(degrees = rotation, pivot = voluteCenter) {
            for (i in 0 until 5) {
                rotate(degrees = i * 72f, pivot = voluteCenter) {
                    val blade = Path().apply {
                        moveTo(voluteCenter.x, voluteCenter.y)
                        quadraticTo(
                            voluteCenter.x + voluteR * 0.44f,
                            voluteCenter.y - voluteR * 0.16f,
                            voluteCenter.x + voluteR * 0.66f,
                            voluteCenter.y - voluteR * 0.42f,
                        )
                    }
                    drawPath(
                        path = blade,
                        color = rotorColor,
                        style = Stroke(width = strokeThin * 1.15f, cap = StrokeCap.Round),
                    )
                }
            }
        }
        // Central shaft hub
        drawCircle(
            color = if (isRunning) palette.gold else casingColor,
            radius = voluteR * 0.20f,
            center = voluteCenter,
        )
        drawCircle(
            color = palette.panel,
            radius = voluteR * 0.08f,
            center = voluteCenter,
        )

        // 5. Clear state medallion badge (top-left) so ON / Ready / OFF(S) / Fault reads instantly
        val badgeCenter = Offset(w * 0.20f, h * 0.20f)
        val badgeR = s * 0.165f
        when {
            isFault -> {
                drawCircle(color = palette.fault, radius = badgeR, center = badgeCenter)
                drawLine(
                    color = Color.White,
                    start = Offset(badgeCenter.x, badgeCenter.y - badgeR * 0.46f),
                    end = Offset(badgeCenter.x, badgeCenter.y + badgeR * 0.10f),
                    strokeWidth = strokeThin,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = Color.White,
                    radius = strokeThin * 0.55f,
                    center = Offset(badgeCenter.x, badgeCenter.y + badgeR * 0.44f),
                )
            }
            isDisabled -> {
                // Crisp neutral-gray Power-OFF medallion (keeps pump silhouette completely unobstructed)
                drawCircle(color = palette.panel, radius = badgeR, center = badgeCenter)
                drawCircle(color = inactiveGray, radius = badgeR, center = badgeCenter, style = Stroke(width = strokeThin))
                val pwrR = badgeR * 0.46f
                drawArc(
                    color = inactiveGray,
                    startAngle = -55f,
                    sweepAngle = 290f,
                    useCenter = false,
                    topLeft = Offset(badgeCenter.x - pwrR, badgeCenter.y - pwrR),
                    size = Size(pwrR * 2f, pwrR * 2f),
                    style = Stroke(width = strokeThin * 0.85f, cap = StrokeCap.Round),
                )
                drawLine(
                    color = inactiveGray,
                    start = Offset(badgeCenter.x, badgeCenter.y - pwrR * 1.15f),
                    end = Offset(badgeCenter.x, badgeCenter.y - pwrR * 0.10f),
                    strokeWidth = strokeThin * 0.85f,
                    cap = StrokeCap.Round,
                )
            }
            isRunning -> {
                drawCircle(color = palette.ok.copy(alpha = 0.28f), radius = badgeR * 1.25f, center = badgeCenter)
                drawCircle(color = palette.ok, radius = badgeR * 0.78f, center = badgeCenter)
                drawCircle(color = Color.White, radius = badgeR * 0.30f, center = badgeCenter)
            }
            isReady -> {
                drawCircle(color = palette.panel, radius = badgeR * 0.82f, center = badgeCenter)
                drawCircle(
                    color = palette.cyan,
                    radius = badgeR * 0.82f,
                    center = badgeCenter,
                    style = Stroke(width = strokeThin),
                )
                drawCircle(color = palette.cyan, radius = badgeR * 0.32f, center = badgeCenter)
            }
        }
    }
}

/** Visual 3-phase Voltmeter analyzer with nominal 230 V / 400 V reference lines (BPC-02.pdf p.10). */
@Composable
fun ThreePhaseVoltageBars(voltages: PhaseVoltages) {
    val palette = LocalScadaPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LtrText("Phase–Neutral (230V Nominal)", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoltageBarTile("VRN", voltages.rn, nominal = 230, maxScale = 280, modifier = Modifier.weight(1f))
            VoltageBarTile("VSN", voltages.sn, nominal = 230, maxScale = 280, modifier = Modifier.weight(1f))
            VoltageBarTile("VTN", voltages.tn, nominal = 230, maxScale = 280, modifier = Modifier.weight(1f))
        }
        LtrText("Phase–Phase (400V Nominal)", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoltageBarTile("VRS", voltages.rs, nominal = 400, maxScale = 480, modifier = Modifier.weight(1f))
            VoltageBarTile("VST", voltages.st, nominal = 400, maxScale = 480, modifier = Modifier.weight(1f))
            VoltageBarTile("VTR", voltages.tr, nominal = 400, maxScale = 480, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun VoltageBarTile(
    label: String,
    volts: Int?,
    nominal: Int,
    maxScale: Int,
    modifier: Modifier = Modifier,
) {
    val palette = LocalScadaPalette.current
    val targetFraction = ((volts ?: 0).toFloat() / maxScale).coerceIn(0f, 1f)
    val fraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "volt_bar_fill",
    )
    val nominalFraction = (nominal.toFloat() / maxScale).coerceIn(0f, 1f)
    val barColor = when {
        volts == null || volts == 0 -> palette.standby
        volts < (nominal * 0.88) || volts > (nominal * 1.10) -> palette.fault
        volts < (nominal * 0.93) || volts > (nominal * 1.06) -> palette.warning
        else -> palette.ok
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.panelElevated)
            .border(0.8.dp, palette.panelBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MonoStyle.copy(fontSize = 10.sp), color = palette.textSecondary)
                Text(
                    volts?.let { "$it V" } ?: "—",
                    style = MonoStyle.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                    color = palette.textPrimary,
                )
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
        ) {
            drawRoundRect(
                color = palette.panelBorder,
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
            if (fraction > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.72f), barColor)),
                    size = Size(size.width * fraction, size.height),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
            val tickX = size.width * nominalFraction
            drawLine(
                color = palette.gold.copy(alpha = 0.75f),
                start = Offset(tickX, 0f),
                end = Offset(tickX, size.height),
                strokeWidth = 1.4.dp.toPx(),
            )
        }
    }
}

/** Realistic glowing PLC panel LED bead. */
@Composable
fun PlcLedBead(
    active: Boolean?,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    activeColor: Color = LocalScadaPalette.current.ok,
    offColor: Color = LocalScadaPalette.current.standby.copy(alpha = 0.35f),
) {
    val color = when (active) {
        true -> activeColor
        false -> offColor
        null -> LocalScadaPalette.current.panelBorder
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, color.copy(alpha = if (active == true) 0.9f else 0.4f), CircleShape),
    )
}


