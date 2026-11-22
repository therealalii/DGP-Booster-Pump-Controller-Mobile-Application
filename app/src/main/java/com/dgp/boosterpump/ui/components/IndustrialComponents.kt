package com.dgp.boosterpump.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.simulation.SimulationScenario
import com.dgp.boosterpump.ui.PersianLabels
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BadgeTone { OK, FAULT, WARNING, NEUTRAL, INFO }

@Composable
fun StatusBadge(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
) {
    val p = LocalScadaPalette.current
    val (fg, bg) = when (tone) {
        BadgeTone.OK -> p.ok to p.okBg
        BadgeTone.FAULT -> p.fault to p.faultBg
        BadgeTone.WARNING -> p.warning to p.warningBg
        BadgeTone.INFO -> p.cyan to p.cyanSoft
        BadgeTone.NEUTRAL -> p.standby to p.standbyBg
    }
    Surface(
        modifier = modifier.border(1.dp, fg.copy(alpha = 0.28f), RoundedCornerShape(8.dp)),
        color = bg,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (pulsing) {
                LivePulseDot(color = fg)
            } else {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(fg),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Live expanding sonar radar dot used for active telemetry links and alarms. */
@Composable
fun LivePulseDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "sonar_dot")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1350, easing = FastOutSlowInEasing)),
        label = "sonar_dot_phase",
    )
    Canvas(modifier = modifier.size(12.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val maxR = size.minDimension / 2f
        drawCircle(
            color = color.copy(alpha = (1f - phase) * 0.65f),
            radius = maxR * (0.45f + phase * 0.55f),
            center = c,
        )
        drawCircle(
            color = color,
            radius = maxR * 0.52f,
            center = c,
        )
    }
}

/**
 * Primary SCADA panel container with an animated metallic light-sweep top accent stripe,
 * optional header icon, steel border, and structured header/body hierarchy.
 */
@Composable
fun ScadaPanel(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentColor: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalScadaPalette.current
    val accent = accentColor ?: p.cyan
    val shimmer = rememberInfiniteTransition(label = "panel_shimmer")
    val sweep by shimmer.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "panel_sweep",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = p.panel),
        border = BorderStroke(1.dp, borderColor ?: p.panelBorder),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (p.isDark) 0.dp else 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Animated SCADA top accent stripe with traveling luminous highlight
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.6.dp),
            ) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.72f), accent.copy(alpha = 0.16f), Color.Transparent),
                    ),
                )
                val cx = size.width * sweep
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, accent.copy(alpha = 0.95f), Color.White.copy(alpha = 0.75f), Color.Transparent),
                        startX = cx - 75.dp.toPx(),
                        endX = cx + 75.dp.toPx(),
                    ),
                )
            }
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = 0.14f))
                                    .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
                            if (subtitle != null) {
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = p.textSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    if (trailing != null) {
                        Spacer(Modifier.width(8.dp))
                        trailing()
                    }
                }
                content()
            }
        }
    }
}

/**
 * Unmistakable top warning banner shown whenever Simulation mode is active so simulated
 * data can never be confused with live controller readings.
 */
@Composable
fun SimulationBanner(
    scenario: SimulationScenario,
    onScenarioSelected: (SimulationScenario) -> Unit,
) {
    val p = LocalScadaPalette.current
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = p.simBannerBg,
        contentColor = p.simBannerFg,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                Column {
                    Text(
                        "حالت شبیه‌سازی — داده واقعی نیست",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        PersianLabels.scenarioLabel(scenario),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box {
                Surface(
                    onClick = { expanded = true },
                    color = p.simBannerFg.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text("سناریو", style = MaterialTheme.typography.labelMedium, color = p.simBannerFg)
                        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    SimulationScenario.entries.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        PersianLabels.scenarioLabel(item),
                                        fontWeight = if (item == scenario) FontWeight.Bold else FontWeight.Normal,
                                    )
                                    Text(
                                        PersianLabels.scenarioDescription(item),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                expanded = false
                                onScenarioSelected(item)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Compact live connection & safety lock telemetry strip beneath the top app bar. */
@Composable
fun ConnectionHeaderStrip(
    state: ConnectionState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
) {
    val p = LocalScadaPalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = p.panel,
        tonalElevation = 1.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val (label, tone) = when (state) {
                            ConnectionState.Disconnected -> "قطع ارتباط" to BadgeTone.NEUTRAL
                            is ConnectionState.Connecting -> "در حال اتصال…" to BadgeTone.WARNING
                            is ConnectionState.Connected -> {
                                val t = if (state.partialError != null) BadgeTone.WARNING else BadgeTone.OK
                                val l = if (state.isSimulated) "متصل (شبیه‌سازی)" else "متصل (آنلاین)"
                                l to t
                            }
                            is ConnectionState.Error -> "خطای ارتباط" to BadgeTone.FAULT
                        }
                        StatusBadge(
                            text = label,
                            tone = tone,
                            pulsing = state is ConnectionState.Connected || state is ConnectionState.Connecting,
                        )
                        Surface(
                            color = p.panelElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, p.panelBorder),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = p.warning, modifier = Modifier.size(12.dp))
                                Text("حالت دستی قفل", style = MaterialTheme.typography.labelSmall, color = p.textSecondary)
                            }
                        }
                    }
                    val detail = when (state) {
                        ConnectionState.Disconnected -> "نمونه‌برداری متوقف است"
                        is ConnectionState.Connecting -> state.target
                        is ConnectionState.Connected -> {
                            val time = state.lastPollMillis?.let(::formatTime) ?: "—"
                            "آخرین خوانش: $time  •  شمارش: ${state.pollCount}"
                        }
                        is ConnectionState.Error -> {
                            val retry = state.retryInSeconds?.let { " — تلاش مجدد تا $it ثانیه" }.orEmpty()
                            "خطا در ارتباط$retry"
                        }
                    }
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state is ConnectionState.Disconnected) {
                    FilledTonalButton(
                        onClick = onConnectClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = p.cyanSoft,
                            contentColor = p.cyan,
                        ),
                    ) { Text("اتصال") }
                } else {
                    FilledTonalButton(
                        onClick = onDisconnectClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = p.faultBg,
                            contentColor = p.fault,
                        ),
                    ) { Text("قطع") }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(p.panelBorder),
            )
        }
    }
}

@Composable
fun NoticeBox(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
) {
    val p = LocalScadaPalette.current
    val (fg, bg) = when (tone) {
        BadgeTone.FAULT -> p.fault to p.faultBg
        BadgeTone.WARNING -> p.warning to p.warningBg
        BadgeTone.OK -> p.ok to p.okBg
        BadgeTone.INFO -> p.cyan to p.cyanSoft
        BadgeTone.NEUTRAL -> p.textSecondary to p.panelElevated
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, fg.copy(alpha = 0.28f), RoundedCornerShape(10.dp)),
        color = bg,
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = fg)
        }
    }
}

/** Forces Left-to-Right rendering for technical strings (hex frames, IPs, equations). */
@Composable
fun LtrText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = LocalScadaPalette.current.textPrimary,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(text = text, style = style, color = color, modifier = modifier)
    }
}

fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(millis))
