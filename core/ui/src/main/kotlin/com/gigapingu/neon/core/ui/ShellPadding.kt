package com.gigapingu.neon.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Heights of HomeShell's floating top app bar and bottom tab bar (including
 * status/navigation bar insets), for list content padding so content starts
 * below the bars but still scrolls behind them. Zero everywhere except inside
 * HomeShell's pager, so pushed/standalone screens and previews are unaffected.
 */
val LocalShellPadding = compositionLocalOf { PaddingValues(0.dp) }
