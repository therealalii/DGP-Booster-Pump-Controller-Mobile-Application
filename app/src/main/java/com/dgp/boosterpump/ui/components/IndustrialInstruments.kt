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
import androidx.compose.foundation.layout.Spacer
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
    maxBar: Double = 10.0,
    drActive: Boolean? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalScadaPalette.current
    val targetFraction = ((pressureBar ?: 0.0) / maxBar).toFloat().coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "pressure_sweep",
    )

    val activeColor = when {
        pressureBar == null -> palette.standby
        pressureBar >= 7.5 -> palette.fault
        pressureBar >= 6.0 -> palette.warning
        pressureBar <= 0.5 -> palette.warning
        else -> palette.cyan
    }

    Box(
        modifier = modifier.size(188.dp),
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
                color = palette.panelBorder.copy(alpha = 0.55f),
                radius = radius + strokePx * 0.85f,
                center = center,
                style = Stroke(width = 1.2.dp.toPx()),
            )

            // Scale tick marks (0..10 bar, major every 2 bar)
            val tickCount = 20
            for (i in 0..tickCount) {
                val deg = startAngle + (totalSweep * i / tickCount)
                val rad = deg * PI / 180.0
                val isMajor = i % 2 == 0
                val rOuter = radius + strokePx * 0.65f
                val rInner = radius + strokePx * (if (isMajor) 0.15f else 0.38f)
                val tickColor = when {
                    i >= 16 -> palette.fault.copy(alpha = 0.7f)
                    i >= 13 -> palette.warning.copy(alpha = 0.7f)
                    else -> palette.textSecondary.copy(alpha = if (isMajor) 0.65f else 0.35f)
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

            // Active sweep + soft glow halo
            if (pressureBar != null && animatedFraction > 0.002f) {
                val sweep = totalSweep * animatedFraction
                drawArc(
                    color = activeColor.copy(alpha = 0.22f),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx + 8.dp.toPx(), cap = StrokeCap.Round),
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

                // Glowing tip indicator dot
                val tipRad = (startAngle + sweep) * PI / 180.0
                val tipOffset = Offset(
                    center.x + (radius * cos(tipRad)).toFloat(),
                    center.y + (radius * sin(tipRad)).toFloat(),
                )
                drawCircle(color = activeColor.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = tipOffset)
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = tipOffset)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "فشار سیستم",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = pressureBar?.let { "%.2f".format(it) } ?: "—",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp),
                    color = palette.textPrimary,
                )
            }
            Text(
                text = "bar (مقیاس ۰–۱۰)",
                style = MaterialTheme.typography.labelSmall,
                color = activeColor,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            val drText = when (drActive) {
                true -> "DR: فعال"
                false -> "DR: خاموش"
                null -> "DR: —"
            }
            Text(
                text = drText,
                style = MaterialTheme.typography.labelSmall,
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
        floaterLow == true && floaterHigh == true -> Triple(palette.cyan, "پر (عادی)", BadgeTone.OK)
        floaterLow == true && floaterHigh == false -> Triple(palette.warning, "سطح میانی", BadgeTone.WARNING)
        floaterLow == false && floaterHigh == false -> Triple(palette.fault, "کمبود آب!", BadgeTone.FAULT)
        floaterLow == false && floaterHigh == true -> Triple(palette.fault, "ناسازگاری فلوتر", BadgeTone.FAULT)
        else -> Triple(palette.standby, "نامشخص", BadgeTone.NEUTRAL)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("مخزن ذخیره", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)

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
                    "Min: %.2f bar".format(minVal),
                    style = MonoStyle.copy(fontSize = 11.sp),
                    color = palette.textSecondary,
                )
                Text(
                    "${samples.size} نمونه زنده",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.cyan,
                )
                Text(
                    "Max: %.2f bar".format(maxVal),
                    style = MonoStyle.copy(fontSize = 11.sp),
                    color = palette.textSecondary,
                )
            }
        }
    }
}

/**
 * Custom-drawn centrifugal booster-pump impeller icon that smoothly rotates while running
 * and highlights in fault crimson when failed.
 */
@Composable
fun AnimatedImpellerIcon(
    running: Boolean?,
    failed: Boolean?,
    size: Dp = 36.dp,
) {
    val palette = LocalScadaPalette.current
    val infinite = rememberInfiniteTransition(label = "impeller")
    val angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "impeller_rot",
    )

    val ringColor = when {
        failed == true -> palette.fault
        running == true -> palette.ok
        else -> palette.panelBorder
    }
    val bladeColor = when {
        failed == true -> palette.fault
        running == true -> palette.cyan
        else -> palette.standby
    }
    val rotation = if (running == true && failed != true) angle else 0f

    Canvas(modifier = Modifier.size(size)) {
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        val r = this.size.minDimension / 2f

        // Outer pump housing
        drawCircle(
            color = ringColor.copy(alpha = if (running == true || failed == true) 0.16f else 0.10f),
            radius = r,
            center = c,
        )
        drawCircle(
            color = ringColor,
            radius = r - 1.5.dp.toPx(),
            center = c,
            style = Stroke(width = 2.dp.toPx()),
        )

        // 4 curved impeller blades
        rotate(degrees = rotation, pivot = c) {
            for (i in 0 until 4) {
                rotate(degrees = i * 90f, pivot = c) {
                    val blade = Path().apply {
                        moveTo(c.x, c.y)
                        quadraticTo(
                            c.x + r * 0.42f,
                            c.y - r * 0.18f,
                            c.x + r * 0.62f,
                            c.y - r * 0.48f,
                        )
                    }
                    drawPath(
                        path = blade,
                        color = bladeColor,
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
        }
        drawCircle(color = bladeColor, radius = r * 0.18f, center = c)
    }
}

/** Visual 3-phase voltage analyzer with nominal 230 V / 400 V reference lines. */
@Composable
fun ThreePhaseVoltageBars(voltages: PhaseVoltages) {
    val palette = LocalScadaPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("فاز به نول (نامی ۲۳۰ ولت)", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoltageBarTile("R–N", voltages.rn, nominal = 230, maxScale = 280, modifier = Modifier.weight(1f))
            VoltageBarTile("S–N", voltages.sn, nominal = 230, maxScale = 280, modifier = Modifier.weight(1f))
            VoltageBarTile("T–N", voltages.tn, nominal = 230, maxScale = 280, modifier = Modifier.weight(1f))
        }
        Text("فاز به فاز (نامی ۴۰۰ ولت)", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoltageBarTile("R–S", voltages.rs, nominal = 400, maxScale = 480, modifier = Modifier.weight(1f))
            VoltageBarTile("S–T", voltages.st, nominal = 400, maxScale = 480, modifier = Modifier.weight(1f))
            VoltageBarTile("T–R", voltages.tr, nominal = 400, maxScale = 480, modifier = Modifier.weight(1f))
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
    val fraction = ((volts ?: 0).toFloat() / maxScale).coerceIn(0f, 1f)
    val nominalFraction = (nominal.toFloat() / maxScale).coerceIn(0f, 1f)
    val barColor = when {
        volts == null || volts == 0 -> palette.standby
        volts < (nominal * 0.88) || volts > (nominal * 1.10) -> palette.fault
        volts < (nominal * 0.93) || volts > (nominal * 1.06) -> palette.warning
        else -> palette.ok
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.panelElevated)
            .border(1.dp, palette.panelBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MonoStyle.copy(fontSize = 10.5.sp), color = palette.textSecondary)
                Text(
                    volts?.let { "$it V" } ?: "—",
                    style = MonoStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = palette.textPrimary,
                )
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp),
        ) {
            drawRoundRect(
                color = palette.panelBorder.copy(alpha = 0.7f),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
            if (fraction > 0f) {
                drawRoundRect(
                    color = barColor,
                    size = Size(size.width * fraction, size.height),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
            val tickX = size.width * nominalFraction
            drawLine(
                color = palette.textPrimary.copy(alpha = 0.6f),
                start = Offset(tickX, 0f),
                end = Offset(tickX, size.height),
                strokeWidth = 1.5.dp.toPx(),
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
