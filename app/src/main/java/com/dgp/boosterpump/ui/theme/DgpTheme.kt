package com.dgp.boosterpump.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Industrial status colours paired with text/icons everywhere they are used. */
object IndustrialColors {
    val OkLight = Color(0xFF166534)
    val OkContainerLight = Color(0xFFDCFCE7)
    val OkDark = Color(0xFF4ADE80)
    val OkContainerDark = Color(0xFF143522)

    val FaultLight = Color(0xFFB91C1C)
    val FaultContainerLight = Color(0xFFFEE2E2)
    val FaultDark = Color(0xFFF87171)
    val FaultContainerDark = Color(0xFF451212)

    val WarningLight = Color(0xFF9A5500)
    val WarningContainerLight = Color(0xFFFEF3C7)
    val WarningDark = Color(0xFFFBBF24)
    val WarningContainerDark = Color(0xFF422806)

    val SimulationBannerBg = Color(0xFFD97706)
    val SimulationBannerFg = Color(0xFF111827)

    val StandbyLight = Color(0xFF475569)
    val StandbyContainerLight = Color(0xFFE2E8F0)
    val StandbyDark = Color(0xFF94A3B8)
    val StandbyContainerDark = Color(0xFF1E293B)
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F4C81),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E6F5),
    onPrimaryContainer = Color(0xFF082B49),
    secondary = Color(0xFF334155),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF08203E),
    primaryContainer = Color(0xFF173A63),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFE2E8F0),
    background = Color(0xFF0B111B),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF451212),
    onErrorContainer = Color(0xFFFECACA),
)

private val IndustrialTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        fontFeatureSettings = "tnum",
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontFeatureSettings = "tnum",
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum",
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontFamily = FontFamily.Monospace,
    ),
)

@Composable
fun DgpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = IndustrialTypography,
        content = content,
    )
}
