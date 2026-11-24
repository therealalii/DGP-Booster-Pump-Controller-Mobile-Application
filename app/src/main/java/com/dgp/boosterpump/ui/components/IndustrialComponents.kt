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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.WarningAmber
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                softWrap = false,
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
 * hairline accent, generous breathing room, and non-truncating title/subtitle/badge header.
 */
@Composable
fun ScadaPanel(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentColor: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    headerBadges: (@Composable () -> Unit)? = null,
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
            // Subtle directional hairline accent across top edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                accent.copy(alpha = 0.55f),
                                accent.copy(alpha = 0.14f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                        .background(accent.copy(alpha = if (p.isDark) 0.12f else 0.10f))
                                        .border(0.8.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(11.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = p.textPrimary,
                                )
                                if (subtitle != null) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 18.sp),
                                        color = p.textSecondary,
                                    )
                                }
                            }
                        }
                        if (trailing != null) {
                            Spacer(Modifier.width(10.dp))
                            trailing()
                        }
                    }
                    if (headerBadges != null) {
                        headerBadges()
                    }
                }
                content()
            }
        }
    }
}

/**
 * Precision animated sliding-pill segmented control (font-scale safe; zero text truncation).
 */
@Composable
fun <T> DgpSegmentedControl(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 44.dp,
) {
    val p = LocalScadaPalette.current
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.4f)
    val effectiveHeight = (height.value * fontScale).dp
    val selectedIndex = items.indexOf(selected).coerceAtLeast(0)
    val count = items.size.coerceAtLeast(1)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(effectiveHeight)
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
                .border(0.9.dp, p.gold.copy(alpha = 0.50f), RoundedCornerShape(9.dp)),
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
                            fontSize = if (count >= 3) 10.5.sp else 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp),
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
            selected -> accent.copy(alpha = 0.58f)
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
            softWrap = false,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
        )
    }
}

/**
 * Signature brushed champagne-gold primary CTA button built on a flexible container (`heightIn`)
 * so text & icon ALWAYS render complete and centered at any screen width or system font scale.
 */
@Composable
fun DgpGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 48.dp,
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
        Brush.horizontalGradient(listOf(Color(0xFFE7B952), Color(0xFFD2992A), Color(0xFFBA811B)))
    } else {
        Brush.horizontalGradient(listOf(p.panelElevated, p.panelElevated))
    }
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .heightIn(min = height)
            .scale(scale)
            .clip(shape)
            .background(gradient)
            .border(
                width = 0.9.dp,
                color = if (enabled) Color(0xFFF5D585).copy(alpha = 0.68f) else p.panelBorder,
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) Color(0xFF0A0702) else p.textMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) Color(0xFF0A0702) else p.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Matching obsidian secondary CTA button with flexible minimum height so labels never clip vertically.
 */
@Composable
fun DgpOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accentColor: Color? = null,
    height: Dp = 46.dp,
) {
    val p = LocalScadaPalette.current
    val fg = when {
        !enabled -> p.textMuted
        accentColor != null -> accentColor
        else -> p.textPrimary
    }
    val borderColor = when {
        !enabled -> p.panelBorder
        accentColor != null -> accentColor.copy(alpha = 0.55f)
        else -> p.panelHighlight
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "outline_btn_scale",
    )
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .heightIn(min = height)
            .scale(scale)
            .clip(shape)
            .background(if (p.isDark) p.panelElevated.copy(alpha = 0.85f) else p.panel)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = fg,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Precision industrial ON/OFF toggle switch for booster pumps and variable-frequency drives.
 *
 * - **ON state:** Vivid emerald track with subtle `I` indicator and solid white circular thumb
 *   (housing the power icon) positioned on the ON side, paired with a bold active label.
 * - **OFF state:** Remains 100% visible and recognizable in neutral inactive gray with subtle `O`
 *   indicator and solid light-gray circular thumb glided smoothly to the opposite OFF side.
 */
@Composable
fun PumpPowerToggle(
    isOn: Boolean?,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onLabel: String = "روشن",
    offLabel: String = "خاموش",
) {
    val p = LocalScadaPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && isOn != null) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pump_toggle_scale",
    )

    val trackWidth = 60.dp
    val trackHeight = 32.dp
    val thumbSize = 24.dp
    val trackInset = 4.dp
    val travel = trackWidth - thumbSize - (trackInset * 2) // 28.dp

    val thumbOffset by animateDpAsState(
        targetValue = when (isOn) {
            true -> travel
            false -> 0.dp
            null -> travel / 2
        },
        animationSpec = spring(
            dampingRatio = 0.74f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pump_toggle_thumb_offset",
    )

    val activeTrack = if (p.isDark) Color(0xFF159A55) else Color(0xFF16A34A)
    val activeBorder = if (p.isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
    val inactiveTrack = if (p.isDark) Color(0xFF3E4758) else Color(0xFFCBD5E1)
    val inactiveBorder = if (p.isDark) Color(0xFF7B879B) else Color(0xFF94A3B8)
    val inactiveLabelColor = if (p.isDark) Color(0xFFB8C2D1) else Color(0xFF475569)

    val trackColor by animateColorAsState(
        targetValue = when (isOn) {
            true -> activeTrack
            false -> inactiveTrack
            null -> p.panelElevated
        },
        animationSpec = tween(220),
        label = "pump_toggle_track_bg",
    )
    val trackBorderColor by animateColorAsState(
        targetValue = when (isOn) {
            true -> activeBorder
            false -> inactiveBorder
            null -> p.panelBorder
        },
        animationSpec = tween(220),
        label = "pump_toggle_track_border",
    )
    val thumbColor by animateColorAsState(
        targetValue = when (isOn) {
            true -> Color(0xFFFFFFFF)
            false -> if (p.isDark) Color(0xFFD8E0EA) else Color(0xFFFFFFFF)
            null -> p.standby
        },
        animationSpec = tween(220),
        label = "pump_toggle_thumb_bg",
    )
    val thumbIconTint by animateColorAsState(
        targetValue = when (isOn) {
            true -> Color(0xFF15803D)
            false -> Color(0xFF334155)
            null -> p.panel
        },
        animationSpec = tween(220),
        label = "pump_toggle_icon_tint",
    )
    val labelColor by animateColorAsState(
        targetValue = when (isOn) {
            true -> p.ok
            false -> inactiveLabelColor
            null -> p.textMuted
        },
        animationSpec = tween(220),
        label = "pump_toggle_label_color",
    )

    Row(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && isOn != null,
                role = Role.Switch,
                onClick = { if (isOn != null) onToggle(!isOn) },
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = when (isOn) {
                true -> onLabel
                false -> offLabel
                null -> "—"
            },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = labelColor,
            maxLines = 1,
        )

        Box(
            modifier = Modifier
                .size(width = trackWidth, height = trackHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(trackColor)
                .border(1.2.dp, trackBorderColor, RoundedCornerShape(999.dp)),
        ) {
            // Subtle IEC 'I' indicator exposed on the Start half when ON
            if (isOn == true) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 15.dp)
                        .size(width = 2.2.dp, height = 10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.85f)),
                )
            }
            // Subtle IEC 'O' indicator exposed on the End half when OFF
            if (isOn == false) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(8.dp)
                        .border(
                            width = 1.6.dp,
                            color = if (p.isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
                            shape = CircleShape,
                        ),
                )
            }

            // Sliding circular toggle indicator with persistent power emblem
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = trackInset)
                    .offset(x = thumbOffset)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(thumbColor)
                    .border(
                        width = 0.9.dp,
                        color = if (isOn == true) Color(0xFFBBF7D0) else Color(0xFF94A3B8),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PowerSettingsNew,
                    contentDescription = if (isOn == true) onLabel else offLabel,
                    tint = thumbIconTint,
                    modifier = Modifier.size(14.dp),
                )
            }
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
 * Flagship floating obsidian-glass bottom navigation dock with a smoothly gliding brushed
 * champagne-gold active capsule tailored to the DGP visual identity.
 */
@Composable
fun DgpBottomNavBar(
    entries: List<NavBarEntry>,
    currentRoute: String,
    onSelect: (String) -> Unit,
) {
    val p = LocalScadaPalette.current
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.3f)
    val dockHeight = (62f * fontScale).dp
    val selectedIndex = entries.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val count = entries.size.coerceAtLeast(1)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(p.canvas)
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        val dockShape = RoundedCornerShape(24.dp)
        val dockBorder = Brush.verticalGradient(
            listOf(
                p.gold.copy(alpha = if (p.isDark) 0.42f else 0.35f),
                p.panelBorder,
            ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (p.isDark) 14.dp else 8.dp,
                    shape = dockShape,
                    ambientColor = Color.Black.copy(alpha = 0.55f),
                    spotColor = p.gold.copy(alpha = 0.18f),
                )
                .clip(dockShape)
                .background(if (p.isDark) Color(0xFF0D111B) else p.panel)
                .border(1.dp, dockBorder, dockShape)
                .padding(5.dp),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dockHeight),
            ) {
                val slotWidth = maxWidth / count
                val pillOffset by animateDpAsState(
                    targetValue = slotWidth * selectedIndex,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "floating_nav_pill_offset",
                )

                // Gliding champagne-gold illuminated active capsule
                Box(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .width(slotWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                if (p.isDark) {
                                    listOf(Color(0xFF332711), Color(0xFF1F170A))
                                } else {
                                    listOf(Color(0xFFFDF3DD), Color(0xFFF7E5BE))
                                },
                            ),
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    p.goldBright.copy(alpha = if (p.isDark) 0.70f else 0.55f),
                                    p.gold.copy(alpha = 0.32f),
                                ),
                            ),
                            shape = RoundedCornerShape(18.dp),
                        ),
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    entries.forEach { entry ->
                        val selected = entry.route == currentRoute
                        val tint by animateColorAsState(
                            targetValue = when {
                                selected && p.isDark -> p.goldBright
                                selected -> p.gold
                                else -> p.textSecondary
                            },
                            animationSpec = tween(200),
                            label = "nav_tint",
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.07f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                            label = "nav_icon_scale",
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = { onSelect(entry.route) },
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                        ) {
                            Icon(
                                imageVector = if (selected) entry.selectedIcon else entry.icon,
                                contentDescription = entry.label,
                                tint = tint,
                                modifier = Modifier
                                    .size(22.dp)
                                    .scale(iconScale),
                            )
                            Text(
                                text = entry.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = tint,
                                softWrap = false,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Unmistakable yet clean demo-mode strip shown whenever Demo / Simulation mode is active
 * so test readings are never confused with live controller telemetry.
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "حالت نمایشی (آزمایشی)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = p.simBannerFg,
                        )
                        Text(
                            PersianLabels.scenarioLabel(scenario),
                            style = MaterialTheme.typography.labelSmall,
                            color = p.simBannerFg.copy(alpha = 0.88f),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box {
                    Surface(
                        onClick = { expanded = true },
                        color = p.simBannerFg.copy(alpha = 0.14f),
                        border = BorderStroke(0.8.dp, p.simBannerFg.copy(alpha = 0.38f)),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text("تغییر حالت", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = p.simBannerFg, softWrap = false)
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

/** Clean, non-truncating connection status ribbon beneath the top bar. */
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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val (label, tone) = when (state) {
                            ConnectionState.Disconnected -> "قطع ارتباط" to BadgeTone.NEUTRAL
                            is ConnectionState.Connecting -> "در حال اتصال…" to BadgeTone.WARNING
                            is ConnectionState.Connected -> {
                                val t = if (state.partialError != null) BadgeTone.WARNING else BadgeTone.OK
                                val l = if (state.isSimulated) "متصل (نمایشی)" else "آنلاین"
                                l to t
                            }
                            is ConnectionState.Error -> "عدم اتصال" to BadgeTone.FAULT
                        }
                        StatusBadge(
                            text = label,
                            tone = tone,
                            pulsing = state is ConnectionState.Connected || state is ConnectionState.Connecting,
                        )
                    }
                    val detail = when (state) {
                        ConnectionState.Disconnected -> "آماده اتصال به کنترلر بوستر پمپ"
                        is ConnectionState.Connecting -> if (state.isSimulated) {
                            "در حال آماده‌سازی حالت نمایشی…"
                        } else {
                            "اتصال به ${state.target.substringBefore(" (")}"
                        }
                        is ConnectionState.Connected -> {
                            val time = state.lastPollMillis?.let(::formatTime) ?: "—"
                            "آخرین بروزرسانی: $time"
                        }
                        is ConnectionState.Error -> {
                            val retry = state.retryInSeconds?.let { " — تلاش مجدد تا $it ثانیه" }.orEmpty()
                            "قطع ارتباط با مبدل شبکه$retry"
                        }
                    }
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = p.textSecondary,
                    )
                }

                Spacer(Modifier.width(10.dp))

                val isDisconnected = state is ConnectionState.Disconnected
                val btnBg = if (isDisconnected) p.goldSoft else p.faultBg
                val btnFg = if (isDisconnected) p.gold else p.fault
                Surface(
                    onClick = if (isDisconnected) onConnectClick else onDisconnectClick,
                    color = btnBg,
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(0.9.dp, btnFg.copy(alpha = 0.45f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
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
                            softWrap = false,
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
