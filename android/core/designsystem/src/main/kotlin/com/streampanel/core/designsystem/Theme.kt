package com.streampanel.core.designsystem

import android.graphics.Color.parseColor
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.streampanel.core.model.AppearanceSettings
import com.streampanel.core.model.ThemeMode

data class StreamPanelExtras(
    val accentGradient: Brush,
    val glowColor: Color,
    val glassAlpha: Float,
    val backgroundImageUrl: String = "",
)

val LocalStreamPanelExtras = staticCompositionLocalOf {
    StreamPanelExtras(
        accentGradient = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF5EEAD4))),
        glowColor = Color(0xFF8B5CF6),
        glassAlpha = 0.78f,
    )
}

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF1E1033),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF042F2E),
    tertiary = Color(0xFFF472B6),
    background = Color(0xFF050508),
    onBackground = Color(0xFFF8F8FF),
    surface = Color(0xFF0E0E16),
    onSurface = Color(0xFFF3F3FC),
    surfaceVariant = Color(0xFF18182A),
    onSurfaceVariant = Color(0xFFC8C8E0),
    outline = Color(0xFF3D3D58),
)

private val MidnightScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    secondary = Color(0xFF38BDF8),
    tertiary = Color(0xFF818CF8),
    background = Color(0xFF020617),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
)

private val OledScheme = darkColorScheme(
    primary = Color(0xFFE5E5E5),
    secondary = Color(0xFFA3A3A3),
    tertiary = Color(0xFF737373),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF171717),
    onSurfaceVariant = Color(0xFFD4D4D4),
)

private val OceanScheme = darkColorScheme(
    primary = Color(0xFF22D3EE),
    secondary = Color(0xFF2DD4BF),
    tertiary = Color(0xFF38BDF8),
    background = Color(0xFF02131A),
    onBackground = Color(0xFFECFEFF),
    surface = Color(0xFF082F49),
    onSurface = Color(0xFFE0F2FE),
    surfaceVariant = Color(0xFF0C4A6E),
    onSurfaceVariant = Color(0xFFBAE6FD),
)

private val SunsetScheme = darkColorScheme(
    primary = Color(0xFFFB923C),
    secondary = Color(0xFFF87171),
    tertiary = Color(0xFFFBBF24),
    background = Color(0xFF1A0A05),
    onBackground = Color(0xFFFFF7ED),
    surface = Color(0xFF2B1208),
    onSurface = Color(0xFFFFEDD5),
    surfaceVariant = Color(0xFF431407),
    onSurfaceVariant = Color(0xFFFED7AA),
)

private val ForestScheme = darkColorScheme(
    primary = Color(0xFF4ADE80),
    secondary = Color(0xFF34D399),
    tertiary = Color(0xFFA3E635),
    background = Color(0xFF041208),
    onBackground = Color(0xFFF0FDF4),
    surface = Color(0xFF0F2418),
    onSurface = Color(0xFFDCFCE7),
    surfaceVariant = Color(0xFF14532D),
    onSurfaceVariant = Color(0xFFBBF7D0),
)

private val NeonScheme = darkColorScheme(
    primary = Color(0xFFF472B6),
    secondary = Color(0xFF22D3EE),
    tertiary = Color(0xFFA78BFA),
    background = Color(0xFF0B0014),
    onBackground = Color(0xFFFAF5FF),
    surface = Color(0xFF1A0528),
    onSurface = Color(0xFFF5D0FE),
    surfaceVariant = Color(0xFF2E1065),
    onSurfaceVariant = Color(0xFFE9D5FF),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF7C3AED),
    secondary = Color(0xFF0D9488),
    tertiary = Color(0xFFDB2777),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
)

fun baseSchemeFor(themeMode: ThemeMode): ColorScheme = when (themeMode) {
    ThemeMode.Dark, ThemeMode.System -> DarkScheme
    ThemeMode.Midnight -> MidnightScheme
    ThemeMode.OLED -> OledScheme
    ThemeMode.Ocean -> OceanScheme
    ThemeMode.Sunset -> SunsetScheme
    ThemeMode.Forest -> ForestScheme
    ThemeMode.Neon -> NeonScheme
    ThemeMode.Light -> LightScheme
}

@Composable
fun StreamPanelTheme(
    appearance: AppearanceSettings = AppearanceSettings(),
    content: @Composable () -> Unit,
) {
    val base = baseSchemeFor(appearance.themeMode)
    val accent = parseHexColor(appearance.accentColor, base.primary)
    val accentSoft = accent.copy(alpha = 0.65f)
    val scheme = base.withAccent(accent)
    val extras = StreamPanelExtras(
        accentGradient = Brush.linearGradient(listOf(accent, accentSoft, base.secondary)),
        glowColor = accent.copy(alpha = 0.35f),
        glassAlpha = if (appearance.enableGlass) 0.82f else 0.96f,
        backgroundImageUrl = appearance.backgroundImageUrl,
    )

    androidx.compose.runtime.CompositionLocalProvider(LocalStreamPanelExtras provides extras) {
        MaterialTheme(
            colorScheme = scheme,
            typography = StreamPanelTypography,
            content = content,
        )
    }
}

fun parseHexColor(value: String?, fallback: Color): Color {
    if (value.isNullOrBlank()) return fallback
    return runCatching { Color(parseColor(value)) }.getOrDefault(fallback)
}

private fun ColorScheme.withAccent(accent: Color): ColorScheme = copy(primary = accent)
