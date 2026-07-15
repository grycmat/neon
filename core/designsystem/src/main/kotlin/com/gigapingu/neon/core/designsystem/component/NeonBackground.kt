package com.gigapingu.neon.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.designsystem.theme.NeonTheme

/**
 * The signature backdrop — three blurred accent orbs over the app bg.
 * Wrap any screen body: NeonBackground { ... }.
 * (The radial gradient already fades to transparent, so on API < 31 where
 * Modifier.blur is a no-op the orbs still look soft.)
 */
@Composable
fun NeonBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = NeonTheme.palette
    Box(modifier = modifier.fillMaxSize().background(palette.bg)) {
        Orb(
            color = palette.pink,
            opacity = palette.orbOpacity,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-80).dp, y = (-60).dp)
                .size(300.dp),
        )
        Orb(
            color = palette.purple,
            opacity = palette.orbOpacity * .95f,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 130.dp, y = 40.dp)
                .size(340.dp),
        )
        Orb(
            color = palette.cyan,
            opacity = palette.orbOpacity * .7f,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 10.dp, y = 140.dp)
                .size(320.dp),
        )
        content()
    }
}

@Composable
private fun Orb(color: Color, opacity: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .blur(48.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = opacity), color.copy(alpha = 0f)),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.minDimension / 2,
                    ),
                )
            },
    )
}
