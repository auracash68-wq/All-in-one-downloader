package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalStreamCleanDark = staticCompositionLocalOf { false }

class StreamCleanColorPalette(
    val primary: Color,
    val background: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val chipInactiveBg: Color,
    val chipInactiveText: Color,
    val iconCircleBg: Color,
    val iconTintGreen: Color,
    val mintBadgeBg: Color,
    val mintBadgeText: Color,
)

val LightPalette = StreamCleanColorPalette(
    primary = PrimaryGreen,
    background = Color(0xFFF7F9F7),
    cardSurface = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFEEF2EE),
    divider = Color(0xFFF1F4F1),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF6B7280),
    textMuted = Color(0xFF9CA3AF),
    chipInactiveBg = Color(0xFFEAEFEA),
    chipInactiveText = Color(0xFF4B5563),
    iconCircleBg = Color(0xFFEBF5EC),
    iconTintGreen = Color(0xFF166534),
    mintBadgeBg = Color(0xFFDCFCE7),
    mintBadgeText = Color(0xFF15803D),
)

val DarkPalette = StreamCleanColorPalette(
    primary = MintGreenPill,
    background = Color(0xFF121613),
    cardSurface = Color(0xFF1B221C),
    cardBorder = Color(0xFF263028),
    divider = Color(0xFF263028),
    textPrimary = Color(0xFFF3F4F6),
    textSecondary = Color(0xFF9CA3AF),
    textMuted = Color(0xFF6B7280),
    chipInactiveBg = Color(0xFF222B24),
    chipInactiveText = Color(0xFF9CA3AF),
    iconCircleBg = Color(0xFF1E2E21),
    iconTintGreen = Color(0xFF78DF9C),
    mintBadgeBg = Color(0xFF064E3B),
    mintBadgeText = Color(0xFFA7F3D0),
)

val LocalStreamCleanPalette = staticCompositionLocalOf { LightPalette }

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = MintGreenLight,
    onPrimaryContainer = MintGreenPillDarkText,
    secondary = MintGreenPill,
    onSecondary = MintGreenPillDarkText,
    background = Color(0xFFF7F9F7),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFEAEFEA),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFEEF2EE)
)

private val DarkColorScheme = darkColorScheme(
    primary = MintGreenPill,
    onPrimary = DarkBackground,
    primaryContainer = PrimaryGreen,
    onPrimaryContainer = Color.White,
    secondary = MintGreenLight,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkSurfaceVariant
)

val StreamCleanShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StreamCleanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val palette = if (darkTheme) DarkPalette else LightPalette

    CompositionLocalProvider(
        LocalStreamCleanDark provides darkTheme,
        LocalStreamCleanPalette provides palette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = StreamCleanShapes,
            content = content
        )
    }
}

// Keep alias for tests & compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    StreamCleanTheme(darkTheme = darkTheme, content = content)
}
