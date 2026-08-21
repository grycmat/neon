package com.gigapingu.neon.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalNeonPalette = staticCompositionLocalOf { NeonPalette.Dark }
val LocalNeonTypography = staticCompositionLocalOf { neonTypography() }
val LocalIconScale = staticCompositionLocalOf { 1f }

/** Access with `NeonTheme.palette` / `NeonTheme.type` / `NeonTheme.iconScale` inside a [NeonTheme] scope. */
object NeonTheme {
    val palette: NeonPalette
        @Composable get() = LocalNeonPalette.current
    val type: NeonTypography
        @Composable get() = LocalNeonTypography.current
    val isLight: Boolean
        @Composable get() = LocalNeonPalette.current.isLight

    /** Multiplier for the post-card action-row icon glyph size — see [com.gigapingu.neon.core.data.IconScale]. */
    val iconScale: Float
        @Composable get() = LocalIconScale.current
}

@Composable
fun NeonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    textScale: Float = 1f,
    iconScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val useDynamicColor = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val palette = remember(darkTheme, useDynamicColor, context) {
        if (useDynamicColor) {
            NeonPalette.dynamic(context, isLight = !darkTheme)
        } else if (darkTheme) {
            NeonPalette.Dark
        } else {
            NeonPalette.Light
        }
    }
    val typography = neonTypography(scale = textScale)
    CompositionLocalProvider(
        LocalNeonPalette provides palette,
        LocalNeonTypography provides typography,
        LocalIconScale provides iconScale,
    ) {
        MaterialTheme(
            colorScheme = neonColorScheme(palette, darkTheme),
            shapes = MaterialTheme.shapes.copy(
                extraLarge = RoundedCornerShape(NeonDims.RadiusCard),
                large = RoundedCornerShape(NeonDims.RadiusButton),
                medium = RoundedCornerShape(NeonDims.RadiusField),
            ),
            content = content,
        )
    }
}

/**
 * Material color scheme derived from the palette, for M3 components (sheets,
 * snackbars, menus…). Accent roles use the ink variants so M3 text/icons that
 * pick them up stay legible on either substrate.
 */
private fun neonColorScheme(p: NeonPalette, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.purpleInk,
        onPrimary = p.onGradient,
        secondary = p.cyanInk,
        onSecondary = p.onGradient,
        tertiary = p.pinkInk,
        onTertiary = p.onGradient,
        error = p.pinkInk,
        background = p.bg,
        onBackground = p.text,
        surface = p.bg,
        onSurface = p.text,
        surfaceVariant = p.surfaceSolid,
        onSurfaceVariant = p.textDim,
        surfaceContainer = p.surfaceSolid,
        surfaceContainerLow = p.surfaceSolid,
        surfaceContainerHigh = p.surfaceSolid,
        surfaceContainerHighest = p.surfaceSolid,
        outline = p.borderStrong,
        outlineVariant = p.divider,
        scrim = p.scrim,
    )
}
