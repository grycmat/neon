package com.gigapingu.neon.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Every color in the app lives here. Two palettes: neon dark (the primary
 * design) and a light variant. The accent trio is shared.
 */
object NeonAccents {
    val Pink = Color(0xFFFF2E8B)
    val Purple = Color(0xFF7B3DFF)
    val Cyan = Color(0xFF22E2FF)
    val PinkDim = Color(0xFFFF6EE5)
}

data class NeonPalette(
    val bg: Color,
    val bg2: Color,
    val surface: Color,
    val surfaceHi: Color,
    val surfaceSolid: Color,
    val border: Color,
    val borderStrong: Color,
    val divider: Color,
    val text: Color,
    val textDim: Color,
    val textMute: Color,
    val onGradient: Color,
    val orbOpacity: Float,
    val shadow: Color,
) {
    // Accent trio (identical in both palettes).
    val pink: Color get() = NeonAccents.Pink
    val purple: Color get() = NeonAccents.Purple
    val cyan: Color get() = NeonAccents.Cyan
    val label: Color get() = NeonAccents.Cyan

    val gradientColors: List<Color>
        get() = listOf(NeonAccents.Pink, NeonAccents.Purple, NeonAccents.Cyan)

    val gradient: Brush get() = Brush.horizontalGradient(gradientColors)

    val gradientSoft: Brush
        get() = Brush.linearGradient(
            listOf(pink.copy(alpha = .2f), purple.copy(alpha = .2f)),
        )

    companion object {
        val Dark = NeonPalette(
            bg = Color(0xFF07060D),
            bg2 = Color(0xFF0D0B16),
            surface = Color(0x0BFFFFFF), // rgba(255,255,255,.045)
            surfaceHi = Color(0x12FFFFFF),
            surfaceSolid = Color(0xFF0D0B16),
            border = Color(0x1CFFFFFF),
            borderStrong = Color(0x33FFFFFF),
            divider = Color(0x12FFFFFF),
            text = Color(0xFFF3F1F8),
            textDim = Color(0xFF9A93AD),
            textMute = Color(0xFF6F6982),
            onGradient = Color.White,
            orbOpacity = .40f,
            shadow = Color(0x8A000000),
        )

        val Light = NeonPalette(
            bg = Color(0xFFF7F5FC),
            bg2 = Color(0xFFFFFFFF),
            surface = Color(0xB3FFFFFF),
            surfaceHi = Color(0xFFFFFFFF),
            surfaceSolid = Color(0xFFFFFFFF),
            border = Color(0x1F1A1030),
            borderStrong = Color(0x331A1030),
            divider = Color(0x141A1030),
            text = Color(0xFF15121C),
            textDim = Color(0xFF6B6577),
            textMute = Color(0xFF9A94A6),
            onGradient = Color.White,
            orbOpacity = .18f,
            shadow = Color(0x2E1A1030),
        )
    }
}
