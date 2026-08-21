package com.gigapingu.neon.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gigapingu.neon.core.designsystem.R

/**
 * Space Grotesk (display) + Manrope (body), bundled as static font files,
 * matching the Flutter google_fonts pairing.
 */
val DisplayFontFamily = FontFamily(
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
)

val BodyFontFamily = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

/**
 * Type roles mirroring the Flutter TextTheme. Color is not baked in here —
 * composables pull the palette from [LocalNeonPalette]; label/dim roles apply
 * their color at the call site exactly like the Flutter theme did.
 */
data class NeonTypography(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val headlineMedium: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
)

/** @param scale multiplier applied to every role's font size, driven by the user's text-size setting. */
fun neonTypography(scale: Float = 1f): NeonTypography {
    val display = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    )
    val body = TextStyle(fontFamily = BodyFontFamily)
    return NeonTypography(
        displayLarge = display.copy(fontSize = 46f.scaled(scale), lineHeight = 1.02.em),
        displayMedium = display.copy(fontSize = 30f.scaled(scale), lineHeight = 1.05.em),
        displaySmall = display.copy(fontSize = 24f.scaled(scale)),
        headlineMedium = display.copy(fontSize = 19f.scaled(scale), letterSpacing = (-0.2).sp),
        titleMedium = body.copy(fontSize = 15f.scaled(scale), fontWeight = FontWeight.ExtraBold),
        titleSmall = body.copy(fontSize = 14f.scaled(scale), fontWeight = FontWeight.ExtraBold),
        bodyLarge = body.copy(fontSize = 15f.scaled(scale), lineHeight = 1.55.em),
        bodyMedium = body.copy(fontSize = 14f.scaled(scale), lineHeight = 1.5.em),
        bodySmall = body.copy(fontSize = 12f.scaled(scale), lineHeight = 1.45.em),
        labelLarge = body.copy(fontSize = 15f.scaled(scale), fontWeight = FontWeight.Bold),
        labelMedium = body.copy(fontSize = 11f.scaled(scale), fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp),
        labelSmall = body.copy(fontSize = 10f.scaled(scale), fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp),
    )
}

private fun Float.scaled(scale: Float) = (this * scale).sp

