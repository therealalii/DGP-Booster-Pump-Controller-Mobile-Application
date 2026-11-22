package com.dgp.boosterpump.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.R
import com.dgp.boosterpump.ui.components.LtrText
import com.dgp.boosterpump.ui.theme.MonoStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val ObsidianBlack = Color(0xFF03050A)
private val ObsidianSurface = Color(0xFF0A0F1B)
private val RoyalGold = Color(0xFFE8B84B)
private val WarmGoldGlow = Color(0xFFFCE08B)
private val CyberCyan = Color(0xFF22D3EE)

/**
 * Full-screen obsidian black animated industrial splash & boot sequence showcasing the
 * dark-themed DGP logo inside counter-rotating metallic gold chronometer & cyber-cyan
 * telemetry rings, sonar pulses, rising ambient particles, and Persian SCADA boot steps.
 */
@Composable
fun SplashLoadingScreen(
    onFinished: () -> Unit,
) {
    val logoScale = remember { Animatable(0.68f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val bootProgress = remember { Animatable(0f) }

    val infinite = rememberInfiniteTransition(label = "splash_kinetics")
    val outerOrbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(11_000, easing = LinearEasing)),
        label = "outer_orbit",
    )
    val innerOrbit by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(4_800, easing = LinearEasing)),
        label = "inner_orbit",
    )
    val sonarPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = FastOutSlowInEasing)),
        label = "sonar_phase",
    )
    val particlePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6_500, easing = LinearEasing)),
        label = "particle_phase",
    )
    val breathGlow by infinite.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1_500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath_glow",
    )
    val shimmerSweep by infinite.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2_200, easing = FastOutSlowInEasing)),
        label = "shimmer_sweep",
    )

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
        launch {
            delay(260)
            textAlpha.animateTo(1f, tween(600))
        }
        bootProgress.animateTo(1f, tween(durationMillis = 2_850, easing = FastOutSlowInEasing))
        delay(220)
        onFinished()
    }

    val pct = (bootProgress.value * 100).toInt().coerceIn(0, 100)
    val bootMessage = when {
        pct < 28 -> "بارگذاری هسته پروتکل مدباس BPC02 (FC01 / FC03 / FC05 / FC06)…"
        pct < 56 -> "راه‌اندازی ماژول شبکه مبدل صنعتی سمین ES 487…"
        pct < 84 -> "بررسی نقشه رجیسترها و لایه‌های قفل ایمنی سخت‌افزار…"
        else -> "سامانه آماده است — انتقال به صفحه ورود اپراتور"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
    ) {
        // Animated dark obsidian SCADA grid + ambient spotlight + rising gold/cyan micro-particles
        ObsidianScadaBackdrop(
            particlePhase = particlePhase,
            breathGlow = breathGlow,
            spotlightYFraction = 0.40f,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top subtle telemetry tag
            Row(
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .clip(RoundedCornerShape(999.dp))
                    .background(ObsidianSurface.copy(alpha = 0.85f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = breathGlow)),
                )
                LtrText(
                    text = "DGP INDUSTRIAL SCADA  •  BPC02 v1.0",
                    style = MonoStyle.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                    color = RoyalGold,
                )
            }

            // Center hero medallion with dark DGP logo & kinetic machinery rings
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(278.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value),
                    contentAlignment = Alignment.Center,
                ) {
                    DgpAnimatedCrest(
                        size = 278.dp,
                        logoSize = 196.dp,
                        outerAngle = outerOrbit,
                        innerAngle = innerOrbit,
                        sonarPhase = sonarPhase,
                        breathGlow = breathGlow,
                        shimmerSweep = shimmerSweep,
                        showSonar = true,
                    )
                }

                Column(
                    modifier = Modifier.alpha(textAlpha.value),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "سامانه هوشمند کنترل بوستر پمپ",
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                        color = Color(0xFFF8FAFC),
                        textAlign = TextAlign.Center,
                    )
                    LtrText(
                        text = "DIGITAL GOSTAR PARSIAN  •  SAMIN ES 487",
                        style = MonoStyle.copy(fontSize = 11.sp, letterSpacing = 1.1.sp),
                        color = RoyalGold.copy(alpha = 0.88f),
                    )
                }
            }

            // Bottom animated boot progress console
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(textAlpha.value),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = bootMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.weight(1f),
                    )
                    LtrText(
                        text = "$pct%",
                        style = MonoStyle.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                        color = RoyalGold,
                    )
                }

                // Glowing gold-to-cyan kinetic progress bar
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                ) {
                    val corner = CornerRadius(6.dp.toPx())
                    drawRoundRect(
                        color = Color(0xFF141D2E),
                        cornerRadius = corner,
                    )
                    val fillW = (size.width * bootProgress.value).coerceAtLeast(0f)
                    if (fillW > 2f) {
                        // Soft outer glow
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(RoyalGold.copy(alpha = 0.35f), CyberCyan.copy(alpha = 0.45f)),
                                startX = 0f,
                                endX = fillW,
                            ),
                            topLeft = Offset(0f, -2.dp.toPx()),
                            size = Size(fillW, size.height + 4.dp.toPx()),
                            cornerRadius = corner,
                        )
                        // Sharp inner bar
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(RoyalGold, WarmGoldGlow, CyberCyan),
                                startX = 0f,
                                endX = fillW,
                            ),
                            size = Size(fillW, size.height),
                            cornerRadius = corner,
                        )
                        // Luminous head spark
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = Offset(fillW - 2.dp.toPx(), size.height / 2f),
                        )
                    }
                }

                Text(
                    text = "تایید سخت‌افزاری Modbus RTU / TCP  •  حفاظت صنعتی ۲۴/۷",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Reusable luxury dark DGP crest with counter-rotating metallic gold chronometer ring,
 * cyber-cyan scanning arc, breathing ambient glow, optional sonar pulses, and diagonal
 * shimmer sweep over `R.drawable.dgp_logo_dark`.
 */
@Composable
fun DgpAnimatedCrest(
    size: Dp,
    logoSize: Dp,
    outerAngle: Float,
    innerAngle: Float,
    sonarPhase: Float,
    breathGlow: Float,
    shimmerSweep: Float,
    showSonar: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxR = this.size.minDimension / 2f

            // Expanding sonar pressure waves
            if (showSonar) {
                for (wave in 0..1) {
                    val phase = (sonarPhase + wave * 0.5f) % 1f
                    val r = maxR * (0.64f + phase * 0.40f)
                    val a = ((1f - phase) * 0.34f).coerceIn(0f, 1f)
                    drawCircle(
                        color = if (wave == 0) RoyalGold.copy(alpha = a) else CyberCyan.copy(alpha = a * 0.85f),
                        radius = r,
                        center = center,
                        style = Stroke(width = 1.6.dp.toPx()),
                    )
                }
            }

            // Deep obsidian radial halo behind the crest
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to RoyalGold.copy(alpha = 0.22f * breathGlow),
                    0.55f to CyberCyan.copy(alpha = 0.11f * breathGlow),
                    0.82f to Color(0xFF060A14).copy(alpha = 0.92f),
                    1.0f to Color.Transparent,
                    center = center,
                    radius = maxR,
                ),
                radius = maxR,
                center = center,
            )

            // Solid obsidian circular medallion base
            val medallionR = maxR * 0.77f
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color(0xFF101726),
                    0.75f to Color(0xFF060911),
                    1.0f to Color(0xFF030509),
                    center = center,
                    radius = medallionR,
                ),
                radius = medallionR,
                center = center,
            )

            // Outer rotating luxury gold chronometer ticks
            val tickR = maxR * 0.89f
            rotate(degrees = outerAngle, pivot = center) {
                val ticks = 48
                for (i in 0 until ticks) {
                    val deg = i * (360f / ticks)
                    val rad = deg * PI / 180.0
                    val major = i % 6 == 0
                    val rIn = tickR - (if (major) 7.dp.toPx() else 3.5.dp.toPx())
                    drawLine(
                        color = RoyalGold.copy(alpha = if (major) 0.85f else 0.30f),
                        start = Offset(
                            center.x + (rIn * cos(rad)).toFloat(),
                            center.y + (rIn * sin(rad)).toFloat(),
                        ),
                        end = Offset(
                            center.x + (tickR * cos(rad)).toFloat(),
                            center.y + (tickR * sin(rad)).toFloat(),
                        ),
                        strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                // Two luxury gold outer arcs
                val arcR = maxR * 0.94f
                val arcTopLeft = Offset(center.x - arcR, center.y - arcR)
                val arcSize = Size(arcR * 2f, arcR * 2f)
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.25f to RoyalGold,
                        0.42f to WarmGoldGlow,
                        0.50f to Color.Transparent,
                        1f to Color.Transparent,
                        center = center,
                    ),
                    startAngle = 0f,
                    sweepAngle = 175f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            // Counter-rotating cyber-cyan precision arc + glowing node spark
            val cyanR = maxR * 0.81f
            rotate(degrees = innerAngle, pivot = center) {
                val tl = Offset(center.x - cyanR, center.y - cyanR)
                val sz = Size(cyanR * 2f, cyanR * 2f)
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.15f to CyberCyan.copy(alpha = 0.25f),
                        0.32f to CyberCyan,
                        0.36f to Color.White,
                        0.38f to Color.Transparent,
                        1f to Color.Transparent,
                        center = center,
                    ),
                    startAngle = 0f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = tl,
                    size = sz,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round),
                )
                // Glowing cyan orbital spark at head (~130 deg)
                val headRad = 130.0 * PI / 180.0
                val head = Offset(
                    center.x + (cyanR * cos(headRad)).toFloat(),
                    center.y + (cyanR * sin(headRad)).toFloat(),
                )
                drawCircle(color = CyberCyan.copy(alpha = 0.45f), radius = 6.dp.toPx(), center = head)
                drawCircle(color = Color.White, radius = 2.6.dp.toPx(), center = head)
            }

            // Thin metallic gold medallion rim
            drawCircle(
                color = RoyalGold.copy(alpha = 0.52f),
                radius = medallionR,
                center = center,
                style = Stroke(width = 1.4.dp.toPx()),
            )
        }

        // Crisp obsidian-themed DGP logo artwork
        Image(
            painter = painterResource(id = R.drawable.dgp_logo_dark),
            contentDescription = "لوگوی دیجیتال گستر پارسیان DGP",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(logoSize)
                .clip(CircleShape),
        )

        // Subtle diagonal metallic shimmer sweeping over the medallion
        Canvas(
            modifier = Modifier
                .size(logoSize)
                .clip(CircleShape),
        ) {
            val sweepX = size.toPx() * shimmerSweep
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        WarmGoldGlow.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                    start = Offset(sweepX - 90f, 0f),
                    end = Offset(sweepX + 90f, size.toPx()),
                ),
            )
        }
    }
}

/**
 * Shared dark obsidian SCADA blueprint grid + radial spotlight + floating gold/cyan particles
 * used across the Splash and Login screens.
 */
@Composable
fun ObsidianScadaBackdrop(
    particlePhase: Float,
    breathGlow: Float,
    spotlightYFraction: Float = 0.30f,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Deep obsidian vertical background gradient
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0xFF04070E),
                0.45f to Color(0xFF03050A),
                1.0f to Color(0xFF060A14),
            ),
        )

        // Subtle precision engineering blueprint grid
        val step = 34.dp.toPx()
        var x = 0f
        while (x <= w) {
            drawLine(
                color = Color(0xFF132038).copy(alpha = 0.28f),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 0.7.dp.toPx(),
            )
            x += step
        }
        var y = 0f
        while (y <= h) {
            drawLine(
                color = Color(0xFF132038).copy(alpha = 0.28f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 0.7.dp.toPx(),
            )
            y += step
        }

        // Ambient radial golden-cyan spotlight behind the crest
        val spotCenter = Offset(w * 0.5f, h * spotlightYFraction)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to RoyalGold.copy(alpha = 0.13f * breathGlow),
                0.42f to CyberCyan.copy(alpha = 0.07f * breathGlow),
                1.0f to Color.Transparent,
                center = spotCenter,
                radius = w * 0.75f,
            ),
            radius = w * 0.75f,
            center = spotCenter,
        )

        // 22 deterministic floating gold & cyan telemetry micro-particles
        val count = 22
        for (i in 0 until count) {
            val seedX = ((i * 73 + 19) % 100) / 100f
            val baseY = ((i * 41 + 7) % 100) / 100f
            val speed = 0.55f + (i % 4) * 0.22f
            val py = ((baseY - particlePhase * speed) % 1f + 1f) % 1f
            val px = (seedX + sin(particlePhase * 2f * PI + i).toFloat() * 0.025f).coerceIn(0.03f, 0.97f)
            val edgeFade = (sin(py * PI)).toFloat().coerceIn(0f, 1f)
            val isGold = i % 2 == 0
            val pColor = if (isGold) RoyalGold else CyberCyan
            val r = (1.3f + (i % 3) * 0.75f).dp.toPx()
            drawCircle(
                color = pColor.copy(alpha = 0.42f * edgeFade),
                radius = r,
                center = Offset(w * px, h * py),
            )
        }
    }
}
