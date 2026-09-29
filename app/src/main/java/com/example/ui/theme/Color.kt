package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// StreamClean Signature Green Palette
val PrimaryGreen = Color(0xFF166534)       // Forest green for main action buttons, logos, active tabs
val PrimaryGreenDark = Color(0xFF14532D)   // Dark forest green for MP4 tab
val PrimaryGreenHover = Color(0xFF15803D)
val MintGreenLight = Color(0xFFA7F3D0)     // Soft mint for paste button & progress track
val MintGreenPill = Color(0xFF78DF9C)      // Vibrant mint for "Video" filter pill & "Paste" button
val MintGreenPillDarkText = Color(0xFF064E3B)

val MintBadgeBg: Color @Composable @ReadOnlyComposable get() = LocalStreamCleanPalette.current.mintBadgeBg
val MintBadgeText: Color @Composable @ReadOnlyComposable get() = LocalStreamCleanPalette.current.mintBadgeText

// Neutral background and surfaces
val AppBackground: Color @Composable @ReadOnlyComposable get() = LocalStreamCleanPalette.current.background
val CardSurface: Color @Composable @ReadOnlyComposable get() = LocalStreamCleanPalette.current.cardSurface
val CardBorder: Color @Composable @ReadOnlyComposable get() = LocalStreamCleanPalette.current.cardBorder
val DividerColor: Color @Composable @ReadOnlyComposable get() = LocalStreamCleanPalette.current.divider

// Typography colors
val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalStreamCleanPalette.current.textPrimary
val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalStreamCleanPalette.current.textSecondary
val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalStreamCleanPalette.current.textMuted

// Chips & Toggles
val ChipInactiveBg: Color @Composable @ReadOnlyComposable get() = LocalStreamCleanPalette.current.chipInactiveBg
val ChipInactiveText: Color @Composable @ReadOnlyComposable get() = LocalStreamCleanPalette.current.chipInactiveText
val IconCircleBg: Color @Composable @ReadOnlyComposable get() = LocalStreamCleanPalette.current.iconCircleBg
val IconTintGreen: Color @Composable @ReadOnlyComposable get() = LocalStreamCleanPalette.current.iconTintGreen

// Dark Theme Constants
val DarkBackground = Color(0xFF121613)
val DarkSurface = Color(0xFF1B221C)
val DarkSurfaceVariant = Color(0xFF263028)
val DarkTextPrimary = Color(0xFFF3F4F6)
val DarkTextSecondary = Color(0xFF9CA3AF)
