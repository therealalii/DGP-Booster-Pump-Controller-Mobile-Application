package com.dgp.boosterpump.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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

private val StageObsidian = Color(0xFF06080D)
private val StageGold = Color(0xFFE2B85E)
private val StageGoldBright = Color(0xFFF6DA8E)
private val StageSteel = Color(0xFF38BDF8)

/**
 * Refined luxury obsidian boot screen featuring the custom beveled DGP medallion inside a
 * precision watchmaker champagne-gold bezel, smooth boot-step transitions, and hairline loader.
 */
@Composable
fun SplashLoadingScreen(
    onFinished: () -> Unit,
) {
    val logoScale = remember { Animatable(0.84f) }
    val logoAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val bootProgress = remember { Animatable(0f) }

    val infinite = rememberInfiniteTransition(label = "splash_luxury")
    val bezelAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
        label = "bezel_angle",
    )
    val tracerAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3_600, easing = LinearEasing)),
        label = "tracer_angle",
    )
    val breath by infinite.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(2_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ambient_breath",
    )
    val shimmer by infinite.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(tween(3_000, easing = FastOutSlowInEasing)),
        label = "crest_shimmer",
    )

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
        launch {
            delay(280)
            contentAlpha.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
        bootProgress.animateTo(1f, tween(durationMillis = 2_650, easing = FastOutSlowInEasing))
        delay(220)
        onFinished()
    }

    val pct = (bootProgress.value * 100).toInt().coerceIn(0, 100)
    val stepIndex = when {
        pct < 28 -> 0
        pct < 58 -> 1
        pct < 86 -> 2
        else -> 3
    }
    val stepLabel = when (stepIndex) {
        0 -> "راه‌اندازی سامانه کنترل بوستر پمپ DGP"
        1 -> "آماده‌سازی ارتباط با کنترلر و مبدل شبکه"
        2 -> "بررسی پایش فشار، حفاظت‌ها و وضعیت پمپ‌ها"
        else -> "آماده بهره‌برداری — انتقال به صفحه ورود"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StageObsidian),
    ) {
        ObsidianBrandBackdrop(
            breathGlow = breath,
            spotlightYFraction = 0.42f,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Quiet refined architectural edition pill
            Row(
                modifier = Modifier
                    .alpha(contentAlpha.value)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF0E121B).copy(alpha = 0.85f))
                    .border(0.8.dp, StageGold.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(StageGold.copy(alpha = breath)),
                )
                LtrText(
                    text = "DIGITAL GOSTAR PARSIAN  •  BPC02",
                    style = MonoStyle.copy(fontSize = 10.sp, letterSpacing = 1.2.sp),
                    color = StageGold.copy(alpha = 0.92f),
                )
            }

            // Centerpiece refined beveled DGP medallion
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                Box(
                    modifier = Modifier
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value),
                    contentAlignment = Alignment.Center,
                ) {
                    DgpAnimatedCrest(
                        size = 248.dp,
                        logoSize = 204.dp,
                        outerAngle = bezelAngle,
                        innerAngle = tracerAngle,
                        breathGlow = breath,
                        shimmerSweep = shimmer,
                        activeTracer = true,
                    )
                }

                Column(
                    modifier = Modifier.alpha(contentAlpha.value),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "کنترل هوشمند بوستر پمپ",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color(0xFFF4F6FA),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "دیجیتال گستر پارسیان  •  پایش و کنترل ایستگاه پمپاژ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF93A0B5),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Minimal luxury hairline progress console
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha.value),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedContent(
                        targetState = stepLabel,
                        transitionSpec = {
                            (fadeIn(tween(260)) + slideInVertically { it / 3 }) togetherWith
                                (fadeOut(tween(180)) + slideOutVertically { -it / 3 })
                        },
                        modifier = Modifier.weight(1f),
                        label = "boot_step_text",
                    ) { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = Color(0xFFA6B2C5),
                        )
                    }
                    LtrText(
                        text = "$pct%",
                        style = MonoStyle.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                        color = StageGold,
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                ) {
                    val corner = CornerRadius(3.dp.toPx())
                    drawRoundRect(
                        color = Color(0x18FFFFFF),
                        cornerRadius = corner,
                    )
                    val fillW = (size.width * bootProgress.value).coerceAtLeast(0f)
                    if (fillW > 1f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(StageGold.copy(alpha = 0.75f), StageGoldBright),
                                startX = 0f,
                                endX = fillW,
                            ),
                            size = Size(fillW, size.height),
                            cornerRadius = corner,
                        )
                    }
                }

                LtrText(
                    text = "DGP BOOSTER PUMP CONTROLLER  •  v2.0",
                    style = MonoStyle.copy(fontSize = 9.5.sp, letterSpacing = 1.1.sp),
                    color = Color(0xFF4F5B70),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

/**
 * Cohesive luxury DGP medallion framing the beveled brushed-gold `dgp_logo_dark` emblem inside a
 * serene watchmaker champagne-gold dial with subtle ambient radial light and optional sweeping arc.
 */
@Composable
fun DgpAnimatedCrest(
    size: Dp,
    logoSize: Dp,
    outerAngle: Float,
    innerAngle: Float,
    breathGlow: Float,
    shimmerSweep: Float,
    activeTracer: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxR = this.size.minDimension / 2f

            // Quiet warm champagne-gold ambient aura behind the medallion
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to StageGold.copy(alpha = 0.16f * breathGlow),
                    0.62f to StageGold.copy(alpha = 0.05f * breathGlow),
                    1.0f to Color.Transparent,
                    center = center,
                    radius = maxR,
                ),
                radius = maxR,
                center = center,
            )

            // Precision hairline outer ring + delicate 60-tick luxury dial
            val dialR = maxR * 0.94f
            drawCircle(
                color = Color(0x16FFFFFF),
                radius = dialR,
                center = center,
                style = Stroke(width = 0.8.dp.toPx()),
            )
            rotate(degrees = outerAngle, pivot = center) {
                val ticks = 60
                for (i in 0 until ticks) {
                    val deg = i * (360f / ticks)
                    val rad = deg * PI / 180.0
                    val major = i % 5 == 0
                    val rIn = dialR - (if (major) 5.dp.toPx() else 2.2.dp.toPx())
                    drawLine(
                        color = StageGold.copy(alpha = if (major) 0.48f else 0.16f),
                        start = Offset(
                            center.x + (rIn * cos(rad)).toFloat(),
                            center.y + (rIn * sin(rad)).toFloat(),
                        ),
                        end = Offset(
                            center.x + (dialR * cos(rad)).toFloat(),
                            center.y + (dialR * sin(rad)).toFloat(),
                        ),
                        strokeWidth = if (major) 1.3.dp.toPx() else 0.8.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Single refined sweeping champagne-gold tracer arc
            if (activeTracer) {
                val arcR = maxR * 0.88f
                rotate(degrees = innerAngle, pivot = center) {
                    val tl = Offset(center.x - arcR, center.y - arcR)
                    val sz = Size(arcR * 2f, arcR * 2f)
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.18f to StageGold.copy(alpha = 0.25f),
                            0.34f to StageGoldBright.copy(alpha = 0.90f),
                            0.37f to Color.Transparent,
                            1.0f to Color.Transparent,
                            center = center,
                        ),
                        startAngle = 0f,
                        sweepAngle = 135f,
                        useCenter = false,
                        topLeft = tl,
                        size = sz,
                        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
        }

        // Refined beveled DGP circular medallion asset
        Image(
            painter = painterResource(id = R.drawable.dgp_logo_dark),
            contentDescription = "لوگوی دیجیتال گستر پارسیان DGP",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(logoSize)
                .clip(CircleShape),
        )

        // Whisper-soft diagonal metallic sheen sweeping gently across the crest
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
                        StageGoldBright.copy(alpha = 0.09f),
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent,
                    ),
                    start = Offset(sweepX - 75f, 0f),
                    end = Offset(sweepX + 75f, size.toPx()),
                ),
            )
        }
    }
}

/**
 * Refined architectural dark obsidian backdrop with soft warm top-center spotlight and whisper-faint
 * precision dot-matrix texture (clean, calm, high-contrast — zero visual clutter).
 */
@Composable
fun ObsidianBrandBackdrop(
    breathGlow: Float,
    spotlightYFraction: Float = 0.25f,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0xFF0A0D14),
                0.45f to Color(0xFF06080D),
                1.0f to Color(0xFF05070B),
            ),
        )

        // Whisper-subtle architectural precision dot matrix
        val spacing = 28.dp.toPx()
        var y = spacing * 0.5f
        while (y < h) {
            var x = spacing * 0.5f
            while (x < w) {
                drawCircle(
                    color = Color(0x0CFFFFFF),
                    radius = 0.85.dp.toPx(),
                    center = Offset(x, y),
                )
                x += spacing
            }
            y += spacing
        }

        // Subtle warm champagne-gold overhead ambient bloom
        val spotCenter = Offset(w * 0.5f, h * spotlightYFraction)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to StageGold.copy(alpha = 0.11f * breathGlow),
                0.48f to StageSteel.copy(alpha = 0.03f * breathGlow),
                1.0f to Color.Transparent,
                center = spotCenter,
                radius = w * 0.82f,
            ),
            radius = w * 0.82f,
            center = spotCenter,
        )
    }
}
