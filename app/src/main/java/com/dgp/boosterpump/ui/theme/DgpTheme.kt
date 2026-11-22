package com.dgp.boosterpump.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.R

/** Vazirmatn — modern geometric Persian + Latin typeface bundled in res/font/. */
val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

@Immutable
data class ScadaPalette(
    val isDark: Boolean,
    val canvas: Color,
    val panel: Color,
    val panelElevated: Color,
    val panelBorder: Color,
    val cyan: Color,
    val cyanSoft: Color,
    val ok: Color,
    val okBg: Color,
    val fault: Color,
    val faultBg: Color,
    val warning: Color,
    val warningBg: Color,
    val standby: Color,
    val standbyBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val terminalBg: Color,
    val terminalText: Color,
    val simBannerBg: Color,
    val simBannerFg: Color,
)

private val DarkScadaPalette = ScadaPalette(
    isDark = true,
    canvas = Color(0xFF070B14),
    panel = Color(0xFF101828),
    panelElevated = Color(0xFF18243A),
    panelBorder = Color(0xFF23324D),
    cyan = Color(0xFF22D3EE),
    cyanSoft = Color(0xFF0E3A4D),
    ok = Color(0xFF34D399),
    okBg = Color(0xFF0A2E24),
    fault = Color(0xFFF87171),
    faultBg = Color(0xFF3F1217),
    warning = Color(0xFFFBBF24),
    warningBg = Color(0xFF3B2606),
    standby = Color(0xFF94A3B8),
    standbyBg = Color(0xFF1E293B),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF94A3B8),
    terminalBg = Color(0xFF050810),
    terminalText = Color(0xFF67E8F9),
    simBannerBg = Color(0xFFF59E0B),
    simBannerFg = Color(0xFF0F172A),
)

private val LightScadaPalette = ScadaPalette(
    isDark = false,
    canvas = Color(0xFFEEF3F9),
    panel = Color(0xFFFFFFFF),
    panelElevated = Color(0xFFF4F7FB),
    panelBorder = Color(0xFFD0DCEB),
    cyan = Color(0xFF0284C7),
    cyanSoft = Color(0xFFE0F2FE),
    ok = Color(0xFF15803D),
    okBg = Color(0xFFDCFCE7),
    fault = Color(0xFFDC2626),
    faultBg = Color(0xFFFEE2E2),
    warning = Color(0xFFB45309),
    warningBg = Color(0xFFFEF3C7),
    standby = Color(0xFF475569),
    standbyBg = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    terminalBg = Color(0xFF0F172A),
    terminalText = Color(0xFF38BDF8),
    simBannerBg = Color(0xFFF59E0B),
    simBannerFg = Color(0xFF0F172A),
)

val LocalScadaPalette = staticCompositionLocalOf { DarkScadaPalette }

private val DarkColors = darkColorScheme(
    primary = Color(0xFF22D3EE),
    onPrimary = Color(0xFF05202E),
    primaryContainer = Color(0xFF0E3A4D),
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF082032),
    secondaryContainer = Color(0xFF18243A),
    onSecondaryContainer = Color(0xFFE2E8F0),
    background = Color(0xFF070B14),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF101828),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF18243A),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF23324D),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF3F1217),
    onErrorContainer = Color(0xFFFECACA),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B4F82),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EAF8),
    onPrimaryContainer = Color(0xFF062944),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2ECF5),
    onSecondaryContainer = Color(0xFF0F172A),
    background = Color(0xFFEEF3F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFD0DCEB),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

private val ScadaTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontFeatureSettings = "tnum",
    ),
    headlineSmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontFeatureSettings = "tnum",
    ),
    titleMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum",
    ),
    labelSmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    ),
)

/** Monospace style for raw hex frames, IP addresses and numeric telemetry. */
val MonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 11.5.sp,
    lineHeight = 16.sp,
    fontFeatureSettings = "tnum",
)

@Composable
fun DgpTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkScadaPalette else LightScadaPalette
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalScadaPalette provides palette,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = ScadaTypography,
            content = content,
        )
    }
}
