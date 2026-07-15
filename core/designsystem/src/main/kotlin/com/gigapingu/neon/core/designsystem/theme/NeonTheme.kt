package com.gigapingu.neon.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalNeonPalette = staticCompositionLocalOf { NeonPalette.Dark }
val LocalNeonTypography = staticCompositionLocalOf { neonTypography() }

/** Access with `NeonTheme.palette` / `NeonTheme.type` inside a [NeonTheme] scope. */
object NeonTheme {
    val palette: NeonPalette
        @Composable get() = LocalNeonPalette.current
    val type: NeonTypography
        @Composable get() = LocalNeonTypography.current
}

@Composable
fun NeonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) NeonPalette.Dark else NeonPalette.Light
    val typography = neonTypography()
    CompositionLocalProvider(
        LocalNeonPalette provides palette,
        LocalNeonTypography provides typography,
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

/** Material color scheme derived from the palette, for M3 components (sheets, snackbars…). */
private fun neonColorScheme(p: NeonPalette, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.purple,
        secondary = p.cyan,
        tertiary = p.pink,
        error = p.pink,
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
        scrim = Color.Black.copy(alpha = .6f),
    )
}
