package com.gigapingu.neon.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gigapingu.neon.core.designsystem.R

/**
 * Space Grotesk (display) + Manrope (body) via downloadable Google Fonts,
 * matching the Flutter google_fonts pairing. Falls back to the system
 * sans-serif while fonts load (or if the fonts provider is unavailable).
 */
private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val spaceGrotesk = GoogleFont("Space Grotesk")
private val manrope = GoogleFont("Manrope")

val DisplayFontFamily = FontFamily(
    Font(googleFont = spaceGrotesk, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = spaceGrotesk, fontProvider = fontProvider, weight = FontWeight.Medium),
)

val BodyFontFamily = FontFamily(
    Font(googleFont = manrope, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = manrope, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = manrope, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = manrope, fontProvider = fontProvider, weight = FontWeight.ExtraBold),
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

