package com.example.adaptiq.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =========================================================================
// Adapt-IQ Premium Research Visual Design System — Color Tokens
// =========================================================================

// Core Backgrounds & Surfaces
val BackgroundDark = Color(0xFF0B1020)
val BackgroundSecondary = Color(0xFF111A2E)
val SurfaceElevated = Color(0xFF172238)
val SurfaceElevatedVariant = Color(0xFF202D45)
val SurfaceCard = Color(0xFF152035)

// Accents
val AccentTeal = Color(0xFF42D6C5)
val AccentTealDark = Color(0xFF1AA899)
val AccentBlue = Color(0xFF7AA7FF)
val AccentBlueDark = Color(0xFF3E6FD8)

// Typography / Content
val TextPrimary = Color(0xFFF4F7FC)
val TextSecondary = Color(0xFFAAB7CC)
val TextMuted = Color(0xFF7284A0)
val TextDark = Color(0xFF06101E)

// Borders & Dividers
val BorderSubtle = Color(0xFF2A3953)
val BorderLight = Color(0xFF384D6E)
val BorderTealFocus = Color(0xFF42D6C5)

// Status & Semantic Feedback
val StatusSuccess = Color(0xFF4ED6A0)
val StatusSuccessContainer = Color(0xFF0D3322)
val StatusWarning = Color(0xFFFFC66D)
val StatusWarningContainer = Color(0xFF3D2A08)
val StatusError = Color(0xFFFF7D86)
val StatusErrorContainer = Color(0xFF3E1216)
val StatusInfo = Color(0xFF7AA7FF)
val StatusInfoContainer = Color(0xFF132247)

// Premium Ambient Gradients
val BackgroundGlowBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF162547),
        BackgroundDark,
        BackgroundDark
    )
)

val CardGradientBrush = Brush.verticalGradient(
    colors = listOf(
        SurfaceElevatedVariant.copy(alpha = 0.85f),
        SurfaceElevated
    )
)

val TealGradientBrush = Brush.horizontalGradient(
    colors = listOf(
        AccentBlue,
        AccentTeal
    )
)

val AccentGlowBrush = Brush.radialGradient(
    colors = listOf(
        AccentTeal.copy(alpha = 0.25f),
        Color.Transparent
    )
)