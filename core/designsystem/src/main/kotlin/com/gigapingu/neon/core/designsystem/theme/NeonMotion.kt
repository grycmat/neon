package com.gigapingu.neon.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Shared motion vocabulary so every animation in the app has one personality:
 * screen-level motion rides a single emphasized curve (enter/exit stay in
 * lockstep), touch feedback uses springs so it feels physical rather than timed.
 */
object NeonMotion {
    /** M3 emphasized curve — screen pushes/pops and large reveals. */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    const val ScreenMs = 420
    const val QuickMs = 220

    /** One spec for both halves of a screen transition. */
    fun <T> screen(): TweenSpec<T> = tween(ScreenMs, easing = Emphasized)

    /** Short fades — top bar title, counters, content crossfades. */
    fun <T> quick(): TweenSpec<T> = tween(QuickMs, easing = Emphasized)

    /** Bouncy spring for icon pops and pressed states. */
    fun bouncy(): SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    /** Clean-settling spring for positions/sizes that shouldn't overshoot. */
    fun settle(): SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
}
