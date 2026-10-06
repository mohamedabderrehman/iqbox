package com.iqbox.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * IQBox Premium Design System
 * Fintech / Creator Platform Identity
 */

// ═══════════════════════════════════════════
// LIGHT THEME
// ═══════════════════════════════════════════

// Backgrounds — soft, not pure white
val BackgroundWhite = Color(0xFFF8FAFC)
val BackgroundGray = Color(0xFFF1F5F9)

// Legacy aliases
val BackgroundDark = Color(0xFF0B1120)
val BackgroundDarkAlt = Color(0xFF111827)

// Primary — Indigo / Blue family
val AccentBlue = Color(0xFF4F46E5)        // Indigo-600
val AccentBlueLight = Color(0xFF6366F1)   // Indigo-500
val AccentBlueDark = Color(0xFF3730A3)    // Indigo-800

// Secondary accent
val AccentCyan = Color(0xFF06B6D4)
val AccentPurple = Color(0xFF7C3AED)

// Earning / Money — Green
val EarningGreen = Color(0xFF059669)       // Emerald-600
val EarningGreenLight = Color(0xFF10B981)  // Emerald-500
val EarningGreenBg = Color(0xFFECFDF5)

// Premium / Gold
val PremiumGold = Color(0xFFF59E0B)
val PremiumGoldLight = Color(0xFFFBBF24)
val PremiumGoldBg = Color(0xFFFFFBEB)

// Cards & Surfaces
val CardBackground = Color(0xFFFFFFFF)
val CardBorder = Color(0xFFE2E8F0)
val SurfaceElevated = Color(0xFFFFFFFF)

// Text — clear hierarchy
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val TextMuted = Color(0xFF94A3B8)
val TextDisabled = Color(0xFFCBD5E1)

// Borders
val BorderLight = Color(0xFFE2E8F0)
val BorderMedium = Color(0xFFCBD5E1)
val DividerColor = Color(0xFFF1F5F9)

// Status
val ErrorColor = Color(0xFFDC2626)
val ErrorBackground = Color(0xFFFEF2F2)
val WarningColor = Color(0xFFF59E0B)
val WarningBackground = Color(0xFFFFFBEB)
val SuccessColor = Color(0xFF059669)
val SuccessBackground = Color(0xFFECFDF5)
val InfoColor = Color(0xFF2563EB)
val InfoBackground = Color(0xFFEFF6FF)

val ShadowColor = Color(0x1A000000)

// ═══════════════════════════════════════════
// DARK THEME — Premium feel
// ═══════════════════════════════════════════
val DarkBackground = Color(0xFF0B1120)
val DarkSurface = Color(0xFF131C2E)
val DarkCard = Color(0xFF1A2540)
val DarkCardElevated = Color(0xFF223050)
val DarkTextPrimary = Color(0xFFF1F5F9)
val DarkTextSecondary = Color(0xFFCBD5E1)
val DarkTextMuted = Color(0xFF64748B)
val DarkBorder = Color(0xFF1E3050)

// ═══════════════════════════════════════════
// GRADIENT PRESETS
// ═══════════════════════════════════════════
val GradientPrimary = listOf(AccentBlue, AccentPurple)
val GradientEarning = listOf(EarningGreen, Color(0xFF34D399))
val GradientPremium = listOf(PremiumGold, Color(0xFFF97316))
val GradientHero = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF9333EA))
val GradientHeroDark = listOf(Color(0xFF312E81), Color(0xFF4C1D95), Color(0xFF581C87))
