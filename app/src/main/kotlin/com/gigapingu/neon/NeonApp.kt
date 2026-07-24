package com.gigapingu.neon

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.gigapingu.neon.core.data.AuthStatus
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.ui.BookmarksKey
import com.gigapingu.neon.core.ui.ComposeKey
import com.gigapingu.neon.core.ui.EditProfileKey
import com.gigapingu.neon.core.ui.FollowListKey
import com.gigapingu.neon.core.ui.HashtagKey
import com.gigapingu.neon.core.ui.HashtagTimelineKey
import com.gigapingu.neon.core.ui.HomeKey
import com.gigapingu.neon.core.ui.MediaPreviewKey
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.ProfileKey
import com.gigapingu.neon.core.ui.LocalTwoPaneEnabled
import com.gigapingu.neon.core.ui.SettingsKey
import com.gigapingu.neon.core.ui.ThreadKey
import com.gigapingu.neon.core.ui.media.MediaPreviewScreen
import com.gigapingu.neon.feature.auth.LoginScreen
import com.gigapingu.neon.feature.composer.ComposeScreen
import com.gigapingu.neon.feature.explore.ExploreScreen
import com.gigapingu.neon.feature.profile.BookmarksScreen
import com.gigapingu.neon.feature.profile.EditProfileScreen
import com.gigapingu.neon.feature.profile.FollowListScreen
import com.gigapingu.neon.feature.profile.ProfileScreen
import com.gigapingu.neon.feature.settings.SettingsScreen
import com.gigapingu.neon.feature.thread.ThreadScreen
import com.gigapingu.neon.feature.timeline.HashtagTimelineScreen

// NavDisplay.DEFAULT_TRANSITION_DURATION_MILLISECOND (internal in alpha05).
private const val NAV_TRANSITION_MS = 400

private fun popSlide(): ContentTransform =
    slideInHorizontally(tween(NAV_TRANSITION_MS)) { -it / 4 } togetherWith
        slideOutHorizontally(tween(NAV_TRANSITION_MS)) { it }

// Composer opens by expanding out of the compose FAB's bottom-end corner,
// and collapses back into it on pop. The near-1f fades keep the screen
// underneath composed and still for the whole transform
// (ExitTransition.KeepUntilTransitionsFinished is internal).
private val ComposerFabOrigin = TransformOrigin(0.9f, 0.93f)

private fun composerEnter(): ContentTransform =
    (scaleIn(
        tween(NAV_TRANSITION_MS),
        initialScale = 0.15f,
        transformOrigin = ComposerFabOrigin,
    ) + fadeIn(tween(NAV_TRANSITION_MS / 2))) togetherWith
        fadeOut(tween(NAV_TRANSITION_MS), targetAlpha = 0.999f)

private fun composerExit(): ContentTransform =
    fadeIn(tween(NAV_TRANSITION_MS), initialAlpha = 0.999f) togetherWith
        (scaleOut(
            tween(NAV_TRANSITION_MS),
            targetScale = 0.15f,
            transformOrigin = ComposerFabOrigin,
        ) + fadeOut(tween(NAV_TRANSITION_MS / 2, delayMillis = NAV_TRANSITION_MS / 2)))

/** Routes between login and the main shell based on auth state (Flutter's _AuthGate). */
@Composable
fun NeonApp(viewModel: ShellViewModel, modifier: Modifier = Modifier) {
    val authStatus by viewModel.authStatus.collectAsStateWithLifecycle()
    Crossfade(targetState = authStatus, label = "authGate") { status ->
        when (status) {
            AuthStatus.Unknown -> NeonBackground(modifier = modifier.fillMaxSize()) {
                val restoreError by viewModel.restoreError.collectAsStateWithLifecycle()
                if (restoreError != null) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                    ) {
                        androidx.compose.material3.Text(
                            text = restoreError ?: "Could not restore account details.",
                            style = NeonTheme.type.bodyMedium,
                            color = NeonTheme.palette.textDim,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        com.gigapingu.neon.core.designsystem.component.GlassButton(
                            label = "Retry",
                            onClick = viewModel::performRestore,
                        )
                    }
                } else {
                    CircularProgressIndicator(
                        color = NeonTheme.palette.cyan,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            AuthStatus.Unauthenticated -> LoginScreen()

            AuthStatus.Authenticated -> AuthenticatedApp(viewModel = viewModel, modifier = modifier)
        }
    }
}

@Composable
private fun AuthenticatedApp(viewModel: ShellViewModel, modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(HomeKey)
    val twoPaneEnabled by viewModel.twoPaneEnabled.collectAsStateWithLifecycle()

    DisposableEffect(backStack) {
        Navigator.backStack = backStack
        Navigator.bindNotificationHandler { statusId, openNotifications ->
            if (statusId != null) {
                Navigator.openThread(statusId)
            } else if (openNotifications) {
                viewModel.selectTab(1)
            }
        }
        onDispose {
            if (Navigator.backStack === backStack) {
                Navigator.backStack = null
                Navigator.unbindNotificationHandler()
            }
        }
    }

    CompositionLocalProvider(LocalTwoPaneEnabled provides twoPaneEnabled) {
        NavDisplay(
            backStack = backStack,
            modifier = modifier.fillMaxSize(),
            onBack = { count -> repeat(count) { backStack.removeLastOrNull() } },
            transitionSpec = {
                slideInHorizontally(tween(NAV_TRANSITION_MS)) { it } togetherWith
                    slideOutHorizontally(tween(NAV_TRANSITION_MS)) { -it / 4 }
            },
            popTransitionSpec = { popSlide() },
            predictivePopTransitionSpec = { popSlide() },
            entryDecorators = listOf(
                rememberSceneSetupNavEntryDecorator(),
                rememberSavedStateNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<HomeKey> { HomeShell(viewModel = viewModel) }
                entry<ThreadKey> { key -> ThreadScreen(statusId = key.statusId) }
                entry<ProfileKey> { key -> ProfileScreen(accountId = key.accountId) }
                entry<HashtagKey> { key -> ExploreScreen(initialQuery = key.query) }
                entry<ComposeKey>(
                    metadata = NavDisplay.transitionSpec { composerEnter() } +
                        NavDisplay.popTransitionSpec { composerExit() } +
                        NavDisplay.predictivePopTransitionSpec { composerExit() },
                ) { key ->
                    ComposeScreen(
                        replyToId = key.replyToId,
                        quotingId = key.quotingId,
                        editStatusId = key.editStatusId,
                        redraftText = key.redraftText,
                        redraftSpoilerText = key.redraftSpoilerText,
                        redraftVisibility = key.redraftVisibility,
                    )
                }
                entry<FollowListKey> { key ->
                    FollowListScreen(
                        accountId = key.accountId,
                        handle = key.handle,
                        following = key.following,
                    )
                }
                entry<EditProfileKey> { EditProfileScreen() }
                entry<SettingsKey> { SettingsScreen() }
                entry<BookmarksKey> { BookmarksScreen() }
                entry<HashtagTimelineKey> { key ->
                    HashtagTimelineScreen(hashtag = key.hashtag)
                }
                entry<MediaPreviewKey> { key ->
                    MediaPreviewScreen(url = key.url, previewUrl = key.previewUrl, type = key.type)
                }
            },
        )
    }
}
