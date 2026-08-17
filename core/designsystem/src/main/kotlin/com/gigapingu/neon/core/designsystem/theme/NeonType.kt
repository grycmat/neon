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

fun neonTypography(): NeonTypography {
    val display = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    )
    val body = TextStyle(fontFamily = BodyFontFamily)
    return NeonTypography(
        displayLarge = display.copy(fontSize = 46.sp, lineHeight = 1.02.em),
        displayMedium = display.copy(fontSize = 30.sp, lineHeight = 1.05.em),
        displaySmall = display.copy(fontSize = 24.sp),
        headlineMedium = display.copy(fontSize = 19.sp, letterSpacing = (-0.2).sp),
        titleMedium = body.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
        titleSmall = body.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
        bodyLarge = body.copy(fontSize = 15.sp, lineHeight = 1.55.em),
        bodyMedium = body.copy(fontSize = 14.sp, lineHeight = 1.5.em),
        bodySmall = body.copy(fontSize = 12.sp, lineHeight = 1.45.em),
        labelLarge = body.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
        labelMedium = body.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp),
        labelSmall = body.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp),
    )
}

