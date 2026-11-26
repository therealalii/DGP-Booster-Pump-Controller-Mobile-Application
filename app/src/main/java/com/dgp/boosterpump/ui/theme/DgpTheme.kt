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

/** Vazirmatn — geometric Persian + Latin typeface bundled in res/font/. */
val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

/**
 * Cohesive DGP Brand & Industrial HMI Design System ("Obsidian Precision & Brushed Champagne Gold").
 * Every surface, glass hairline, gold accent, and hydraulic telemetry tone is derived directly
 * from the refined DGP crest so logo and interface read as one unified commercial product.
 */
@Immutable
data class ScadaPalette(
    val isDark: Boolean,
    val canvas: Color,
    val panel: Color,
    val panelElevated: Color,
    val panelBorder: Color,
    val panelHighlight: Color,
    val hairline: Color,
    val gold: Color,
    val goldBright: Color,
    val goldSoft: Color,
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
    val textMuted: Color,
    val terminalBg: Color,
    val terminalText: Color,
    val simBannerBg: Color,
    val simBannerFg: Color,
)

private val DarkScadaPalette = ScadaPalette(
    isDark = true,
    canvas = Color(0xFF04060A),
    panel = Color(0xFF131A27),
    panelElevated = Color(0xFF1B2436),
    panelBorder = Color(0x30FFFFFF),
    panelHighlight = Color(0x4AFFFFFF),
    hairline = Color(0x1EFFFFFF),
    gold = Color(0xFFE2B85E),
    goldBright = Color(0xFFF5D68A),
    goldSoft = Color(0xFF282012),
    cyan = Color(0xFF38BDF8),
    cyanSoft = Color(0xFF0E2A3F),
    ok = Color(0xFF34D399),
    okBg = Color(0xFF0B281F),
    fault = Color(0xFFFB7185),
    faultBg = Color(0xFF34121B),
    warning = Color(0xFFFBBF24),
    warningBg = Color(0xFF2E220A),
    standby = Color(0xFF94A3B8),
    standbyBg = Color(0xFF1D2636),
    textPrimary = Color(0xFFF4F6FA),
    textSecondary = Color(0xFFA0AEC4),
    textMuted = Color(0xFF6B7890),
    terminalBg = Color(0xFF080C14),
    terminalText = Color(0xFF7DD3FC),
    simBannerBg = Color(0xFF2C200B),
    simBannerFg = Color(0xFFFCD34D),
)

private val LightScadaPalette = ScadaPalette(
    isDark = false,
    canvas = Color(0xFFE5ECF4),
    panel = Color(0xFFFFFFFF),
    panelElevated = Color(0xFFF1F5FA),
    panelBorder = Color(0xFFC2CFE0),
    panelHighlight = Color(0xFFFFFFFF),
    hairline = Color(0xFFD9E2EE),
    gold = Color(0xFFB58425),
    goldBright = Color(0xFFD4A23E),
    goldSoft = Color(0xFFFDF5E6),
    cyan = Color(0xFF0284C7),
    cyanSoft = Color(0xFFE0F2FE),
    ok = Color(0xFF15803D),
    okBg = Color(0xFFDCFCE7),
    fault = Color(0xFFDC2626),
    faultBg = Color(0xFFFEE2E2),
    warning = Color(0xFFB45309),
    warningBg = Color(0xFFFEF3C7),
    standby = Color(0xFF475569),
    standbyBg = Color(0xFFE6ECF4),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF4B5A70),
    textMuted = Color(0xFF7B8A9E),
    terminalBg = Color(0xFF0B0F17),
    terminalText = Color(0xFF7DD3FC),
    simBannerBg = Color(0xFFFEF3C7),
    simBannerFg = Color(0xFF92400E),
)

val LocalScadaPalette = staticCompositionLocalOf { DarkScadaPalette }

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE2B85E),
    onPrimary = Color(0xFF0F0B04),
    primaryContainer = Color(0xFF2B2212),
    onPrimaryContainer = Color(0xFFFCE7B2),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF061E2E),
    secondaryContainer = Color(0xFF142436),
    onSecondaryContainer = Color(0xFFE0F2FE),
    background = Color(0xFF04060A),
    onBackground = Color(0xFFF4F6FA),
    surface = Color(0xFF131A27),
    onSurface = Color(0xFFF4F6FA),
    surfaceVariant = Color(0xFF1B2436),
    onSurfaceVariant = Color(0xFFA0AEC4),
    outline = Color(0xFF334157),
    outlineVariant = Color(0xFF232E42),
    error = Color(0xFFFB7185),
    onError = Color(0xFF3F0A14),
    errorContainer = Color(0xFF34121B),
    onErrorContainer = Color(0xFFFFD6DC),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFB58425),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFDF5E6),
    onPrimaryContainer = Color(0xFF3D2A05),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF082F49),
    background = Color(0xFFE5ECF4),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5FA),
    onSurfaceVariant = Color(0xFF4B5A70),
    outline = Color(0xFFC2CFE0),
    outlineVariant = Color(0xFFD9E2EE),
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
        lineHeight = 29.sp,
        fontFeatureSettings = "tnum",
    ),
    titleMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.5.sp,
        lineHeight = 23.sp,
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
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum",
    ),
    labelSmall = TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 15.sp,
    ),
)

/** Crisp monospace style with tabular numerals for hex frames, IPs, and engineering values. */
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
