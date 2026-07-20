package com.gigapingu.neon.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.designsystem.theme.NeonTheme

/**
 * Big-screen (unfolded foldable / tablet) support. One width threshold drives
 * every adaptive layout: past it HomeShell swaps the bottom tab bar for a left
 * nav rail and screens go two-pane, with a 3:2 ratio between the left and right panes.
 */
const val BigScreenMinWidthDp = 640

/** Width of HomeShell's left nav rail on big screens. */
val ShellRailWidth = 76.dp

@Composable
fun isBigScreen(): Boolean = LocalConfiguration.current.screenWidthDp >= BigScreenMinWidthDp

/**
 * Left-pane width for a two-pane layout (3:2 ratio). [inShell] subtracts the nav rail
 * sitting left of tab content; standalone (pushed) screens span the full window.
 */
@Composable
fun hingePaneWidth(inShell: Boolean = false): Dp =
    (LocalConfiguration.current.screenWidthDp * 3 / 5).dp - (if (inShell) ShellRailWidth else 0.dp)

/**
 * Gradient edge marker for the row currently open in a list-detail pane.
 * Wraps the row content; the bar hangs in the list's start content padding.
 */
@Composable
fun PaneSelection(selected: Boolean, content: @Composable () -> Unit) {
    if (!selected) {
        content()
        return
    }
    val palette = NeonTheme.palette
    Box {
        content()
        // matchParentSize: sized by the row, so the bar never influences layout.
        Box(Modifier.matchParentSize().padding(vertical = 21.dp)) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-9).dp)
                    .fillMaxHeight()
                    .width(3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.verticalGradient(palette.gradientColors)),
            )
        }
    }
}
