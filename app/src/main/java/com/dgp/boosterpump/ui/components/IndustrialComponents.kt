package com.dgp.boosterpump.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.simulation.SimulationScenario
import com.dgp.boosterpump.ui.theme.IndustrialColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BadgeTone { OK, FAULT, WARNING, NEUTRAL }

@Composable
fun StatusBadge(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val (fg, bg) = when (tone) {
        BadgeTone.OK -> if (dark) IndustrialColors.OkDark to IndustrialColors.OkContainerDark
        else IndustrialColors.OkLight to IndustrialColors.OkContainerLight
        BadgeTone.FAULT -> if (dark) IndustrialColors.FaultDark to IndustrialColors.FaultContainerDark
        else IndustrialColors.FaultLight to IndustrialColors.FaultContainerLight
        BadgeTone.WARNING -> if (dark) IndustrialColors.WarningDark to IndustrialColors.WarningContainerDark
        else IndustrialColors.WarningLight to IndustrialColors.WarningContainerLight
        BadgeTone.NEUTRAL -> if (dark) IndustrialColors.StandbyDark to IndustrialColors.StandbyContainerDark
        else IndustrialColors.StandbyLight to IndustrialColors.StandbyContainerLight
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg),
            )
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

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor ?: MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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

/**
 * Unmistakable top banner shown whenever simulation data is active.
 * Simulation data must never be mistaken for live controller readings.
 */
@Composable
fun SimulationBanner(
    scenario: SimulationScenario,
    onScenarioSelected: (SimulationScenario) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = IndustrialColors.SimulationBannerBg,
        contentColor = IndustrialColors.SimulationBannerFg,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
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
                        "SIMULATION MODE — NOT LIVE HARDWARE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        scenario.label,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Box {
                TextButton(onClick = { expanded = true }) {
                    Text(
                        "Scenario",
                        color = IndustrialColors.SimulationBannerFg,
                        fontWeight = FontWeight.Bold,
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    SimulationScenario.entries.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(item.label, fontWeight = if (item == scenario) FontWeight.Bold else FontWeight.Normal)
                                    Text(
                                        item.description,
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

/** Compact strip showing live link status and read-only safety lock. */
@Composable
fun ConnectionHeaderStrip(
    state: ConnectionState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val (label, tone) = when (state) {
                        ConnectionState.Disconnected -> "Disconnected" to BadgeTone.NEUTRAL
                        is ConnectionState.Connecting -> "Connecting…" to BadgeTone.WARNING
                        is ConnectionState.Connected -> {
                            val t = if (state.partialError != null) BadgeTone.WARNING else BadgeTone.OK
                            val l = if (state.isSimulated) "Simulated" else "Connected"
                            l to t
                        }
                        is ConnectionState.Error -> "Comm error" to BadgeTone.FAULT
                    }
                    StatusBadge(label, tone)
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                            Text("READ-ONLY", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                val detail = when (state) {
                    ConnectionState.Disconnected -> "Not polling"
                    is ConnectionState.Connecting -> state.target
                    is ConnectionState.Connected -> {
                        val time = state.lastPollMillis?.let(::formatTime) ?: "—"
                        "${state.target} • Last read $time"
                    }
                    is ConnectionState.Error -> {
                        val retry = state.retryInSeconds?.let { " (retry in ${it}s)" }.orEmpty()
                        "${state.target}$retry"
                    }
                }
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state is ConnectionState.Disconnected) {
                TextButton(onClick = onConnectClick) { Text("Connect") }
            } else {
                TextButton(onClick = onDisconnectClick) { Text("Disconnect") }
            }
        }
    }
}

@Composable
fun NoticeBox(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val (fg, bg) = when (tone) {
        BadgeTone.FAULT -> if (dark) IndustrialColors.FaultDark to IndustrialColors.FaultContainerDark
        else IndustrialColors.FaultLight to IndustrialColors.FaultContainerLight
        BadgeTone.WARNING -> if (dark) IndustrialColors.WarningDark to IndustrialColors.WarningContainerDark
        else IndustrialColors.WarningLight to IndustrialColors.WarningContainerLight
        BadgeTone.OK -> if (dark) IndustrialColors.OkDark to IndustrialColors.OkContainerDark
        else IndustrialColors.OkLight to IndustrialColors.OkContainerLight
        BadgeTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bg,
        shape = RoundedCornerShape(8.dp),
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

fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(millis))
