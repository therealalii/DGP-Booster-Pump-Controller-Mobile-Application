package com.dgp.boosterpump.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.simulation.SimulationScenario
import com.dgp.boosterpump.ui.PersianLabels
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BadgeTone { OK, FAULT, WARNING, NEUTRAL, INFO, GOLD }

/**
 * Refined status pill with crisp translucent surface, hairline border, and optional live sonar dot.
 */
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
        BadgeTone.GOLD -> p.gold to p.goldSoft
        BadgeTone.NEUTRAL -> p.standby to p.standbyBg
    }
    Surface(
        modifier = modifier.border(0.8.dp, fg.copy(alpha = 0.28f), RoundedCornerShape(999.dp)),
        color = bg,
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (pulsing) {
                LivePulseDot(color = fg)
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(fg),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Subtle expanding radar sonar indicator used sparingly for active online links and critical faults. */
@Composable
fun LivePulseDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "sonar_dot")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing)),
        label = "sonar_dot_phase",
    )
    Canvas(modifier = modifier.size(10.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val maxR = size.minDimension / 2f
        drawCircle(
            color = color.copy(alpha = (1f - phase) * 0.55f),
            radius = maxR * (0.45f + phase * 0.55f),
            center = c,
        )
        drawCircle(
            color = color,
            radius = maxR * 0.54f,
            center = c,
        )
    }
}

/**
 * Flagship architectural surface card with top-edge specular glass-bevel border, quiet precision
 * hairline accent, generous breathing room, and structured title/subtitle/trailing header.
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
    val accent = accentColor ?: p.gold
    val shape = RoundedCornerShape(20.dp)
    val borderBrush = if (borderColor != null) {
        Brush.verticalGradient(listOf(borderColor, borderColor.copy(alpha = 0.45f)))
    } else {
        Brush.verticalGradient(listOf(p.panelHighlight, p.panelBorder))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = p.panel),
        border = BorderStroke(1.dp, borderBrush),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = if (p.isDark) 0.dp else 1.5.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Subtle directional hairline accent across top edge (clean, non-distracting)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                accent.copy(alpha = 0.55f),
                                accent.copy(alpha = 0.12f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        if (icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(accent.copy(alpha = if (p.isDark) 0.11f else 0.10f))
                                    .border(0.8.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(11.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(title, style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
                            if (subtitle != null) {
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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
 * Precision animated sliding-pill segmented control (replaces raw M3 FilterChip rows).
 * Works cleanly in RTL layouts by computing slot offset in physical coordinates.
 */
@Composable
fun <T> DgpSegmentedControl(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 40.dp,
) {
    val p = LocalScadaPalette.current
    val selectedIndex = items.indexOf(selected).coerceAtLeast(0)
    val count = items.size.coerceAtLeast(1)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(p.panelElevated)
            .border(0.8.dp, p.panelBorder, RoundedCornerShape(12.dp))
            .padding(3.dp),
    ) {
        val segmentWidth = maxWidth / count
        val animatedOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "segment_pill_offset",
        )

        // Sliding champagne-gold-tinted active pill
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(9.dp))
                .background(if (p.isDark) p.goldSoft else p.panel)
                .border(0.9.dp, p.gold.copy(alpha = 0.45f), RoundedCornerShape(9.dp)),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEach { item ->
                val isSelected = item == selected
                val textColor by animateColorAsState(
                    targetValue = when {
                        !enabled -> p.textMuted
                        isSelected -> p.gold
                        else -> p.textSecondary
                    },
                    animationSpec = tween(200),
                    label = "seg_color",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .clickable(enabled = enabled && !isSelected) { onSelect(item) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(item),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }
        }
    }
}

/** Refined tactile pill chip for horizontally scrollable filters, presets, and secondary options. */
@Composable
fun DgpPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = LocalScadaPalette.current.gold,
) {
    val p = LocalScadaPalette.current
    val bg by animateColorAsState(
        targetValue = when {
            selected -> accent.copy(alpha = if (p.isDark) 0.16f else 0.13f)
            else -> p.panelElevated
        },
        animationSpec = tween(180),
        label = "chip_bg",
    )
    val border by animateColorAsState(
        targetValue = when {
            selected -> accent.copy(alpha = 0.55f)
            else -> p.panelBorder
        },
        animationSpec = tween(180),
        label = "chip_border",
    )
    val fg by animateColorAsState(
        targetValue = when {
            !enabled -> p.textMuted
            selected -> accent
            else -> p.textSecondary
        },
        animationSpec = tween(180),
        label = "chip_fg",
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(0.9.dp, border),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = fg,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
        )
    }
}

/** Signature brushed champagne-gold primary CTA button with tactile press-scale feedback. */
@Composable
fun DgpGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 46.dp,
) {
    val p = LocalScadaPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "gold_btn_scale",
    )
    val gradient = if (enabled) {
        Brush.horizontalGradient(listOf(p.goldBright, p.gold, Color(0xFFC89635)))
    } else {
        Brush.horizontalGradient(listOf(p.panelElevated, p.panelElevated))
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(13.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        modifier = modifier
            .height(height)
            .scale(scale)
            .clip(RoundedCornerShape(13.dp))
            .background(gradient)
            .border(
                0.8.dp,
                if (enabled) p.goldBright.copy(alpha = 0.55f) else p.panelBorder,
                RoundedCornerShape(13.dp),
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) Color(0xFF0E0A03) else p.textMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) Color(0xFF0E0A03) else p.textMuted,
                maxLines = 1,
            )
        }
    }
}

/** Bespoke bottom navigation item descriptor. */
data class NavBarEntry(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

/**
 * Custom docked obsidian-glass bottom navigation bar with smooth champagne-gold active pill
 * indicator and spring icon feedback (replaces stock M3 NavigationBar).
 */
@Composable
fun DgpBottomNavBar(
    entries: List<NavBarEntry>,
    currentRoute: String,
    onSelect: (String) -> Unit,
) {
    val p = LocalScadaPalette.current
    Surface(
        color = p.panel,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Subtle illuminated hairline along top edge of dock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                p.panelBorder,
                                p.gold.copy(alpha = 0.35f),
                                p.panelBorder,
                            ),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                entries.forEach { entry ->
                    val selected = entry.route == currentRoute
                    val tint by animateColorAsState(
                        targetValue = if (selected) p.gold else p.textSecondary,
                        animationSpec = tween(200),
                        label = "nav_tint",
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "nav_scale",
                    )
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (selected) 24.dp else 0.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "nav_bar_w",
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelect(entry.route) },
                            )
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .height(2.5.dp)
                                .width(indicatorWidth)
                                .clip(RoundedCornerShape(999.dp))
                                .background(p.gold),
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) p.goldSoft else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (selected) entry.selectedIcon else entry.icon,
                                contentDescription = entry.label,
                                tint = tint,
                                modifier = Modifier
                                    .size(21.dp)
                                    .scale(iconScale),
                            )
                        }
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Unmistakable yet refined simulation warning strip shown whenever Simulation mode is active
 * so simulated readings can never be mistaken for live controller telemetry.
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
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                    Column {
                        Text(
                            "حالت شبیه‌سازی — داده واقعی نیست",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = p.simBannerFg,
                        )
                        Text(
                            PersianLabels.scenarioLabel(scenario),
                            style = MaterialTheme.typography.labelSmall,
                            color = p.simBannerFg.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box {
                    Surface(
                        onClick = { expanded = true },
                        color = p.simBannerFg.copy(alpha = 0.14f),
                        border = BorderStroke(0.8.dp, p.simBannerFg.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text("سناریو", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = p.simBannerFg)
                            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, modifier = Modifier.size(15.dp))
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(p.simBannerFg.copy(alpha = 0.25f)),
            )
        }
    }
}

/** Compact, single-row industrial telemetry ribbon beneath the header. */
@Composable
fun ConnectionHeaderStrip(
    state: ConnectionState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
) {
    val p = LocalScadaPalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = p.canvas,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
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
                                val l = if (state.isSimulated) "متصل (شبیه‌سازی)" else "آنلاین"
                                l to t
                            }
                            is ConnectionState.Error -> "خطای لینک" to BadgeTone.FAULT
                        }
                        StatusBadge(
                            text = label,
                            tone = tone,
                            pulsing = state is ConnectionState.Connected || state is ConnectionState.Connecting,
                        )
                        Surface(
                            color = p.panel,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(0.8.dp, p.panelBorder),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = p.gold, modifier = Modifier.size(11.dp))
                                Text("کنترل دستی قفل", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = p.textSecondary)
                            }
                        }
                    }
                    val detail = when (state) {
                        ConnectionState.Disconnected -> "آماده اتصال به کنترلر BPC02"
                        is ConnectionState.Connecting -> state.target
                        is ConnectionState.Connected -> {
                            val time = state.lastPollMillis?.let(::formatTime) ?: "—"
                            "بروزرسانی: $time  •  فریم: ${state.pollCount}"
                        }
                        is ConnectionState.Error -> {
                            val retry = state.retryInSeconds?.let { " — تلاش مجدد تا $it ثانیه" }.orEmpty()
                            "قطع ارتباط با گیت‌وی$retry"
                        }
                    }
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = p.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val isDisconnected = state is ConnectionState.Disconnected
                val btnBg = if (isDisconnected) p.goldSoft else p.faultBg
                val btnFg = if (isDisconnected) p.gold else p.fault
                Surface(
                    onClick = if (isDisconnected) onConnectClick else onDisconnectClick,
                    color = btnBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.9.dp, btnFg.copy(alpha = 0.42f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PowerSettingsNew,
                            contentDescription = null,
                            tint = btnFg,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = if (isDisconnected) "اتصال" else "قطع",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = btnFg,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(p.hairline),
            )
        }
    }
}

/** Refined contextual callout box with matching semantic icon. */
@Composable
fun NoticeBox(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
) {
    val p = LocalScadaPalette.current
    val (fg, bg, icon) = when (tone) {
        BadgeTone.FAULT -> Triple(p.fault, p.faultBg, Icons.Outlined.WarningAmber)
        BadgeTone.WARNING -> Triple(p.warning, p.warningBg, Icons.Outlined.WarningAmber)
        BadgeTone.OK -> Triple(p.ok, p.okBg, Icons.Outlined.CheckCircleOutline)
        BadgeTone.INFO -> Triple(p.cyan, p.cyanSoft, Icons.Outlined.Info)
        BadgeTone.GOLD -> Triple(p.gold, p.goldSoft, Icons.Outlined.Info)
        BadgeTone.NEUTRAL -> Triple(p.textSecondary, p.panelElevated, Icons.Outlined.Info)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(0.8.dp, fg.copy(alpha = 0.28f), RoundedCornerShape(12.dp)),
        color = bg,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(17.dp))
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
