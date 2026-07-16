package com.gigapingu.neon.core.designsystem.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Shared-element ("hero") plumbing. The app shell provides both scopes —
 * the SharedTransitionLayout wrapping the nav graph and the current nav
 * entry's AnimatedContent scope — and components mark hero views with
 * [neonSharedElement]. Degrades to a no-op when either scope is absent
 * (previews, HomeShell tabs outside a transition).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Marks this element as the hero identified by [key] across nav entries. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.neonSharedElement(key: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(sharedScope) {
        this@neonSharedElement.sharedElement(
            rememberSharedContentState(key),
            animatedScope,
        )
    }
}
