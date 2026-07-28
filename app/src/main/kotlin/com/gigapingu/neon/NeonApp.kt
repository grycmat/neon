package com.gigapingu.neon

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.gigapingu.neon.core.ui.BlockedAccountsKey
import com.gigapingu.neon.core.ui.BookmarksKey
import com.gigapingu.neon.core.ui.ComposeKey
import com.gigapingu.neon.core.ui.EditProfileKey
import com.gigapingu.neon.core.ui.FollowListKey
import com.gigapingu.neon.core.ui.HashtagKey
import com.gigapingu.neon.core.ui.HashtagTimelineKey
import com.gigapingu.neon.core.ui.FiltersKey
import com.gigapingu.neon.core.ui.HomeKey
import com.gigapingu.neon.core.ui.ListTimelineKey
import com.gigapingu.neon.core.ui.ManageFollowedHashtagsKey
import com.gigapingu.neon.core.ui.ManageListsKey
import com.gigapingu.neon.core.ui.MediaPreviewKey
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.NewMessageKey
import com.gigapingu.neon.core.ui.NotificationRequestsKey
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
import com.gigapingu.neon.feature.settings.BlockedAccountsScreen
import com.gigapingu.neon.feature.settings.FiltersScreen
import com.gigapingu.neon.feature.settings.ManageFollowedHashtagsScreen
import com.gigapingu.neon.feature.settings.ManageListsScreen
import com.gigapingu.neon.feature.settings.SettingsScreen
import com.gigapingu.neon.feature.messages.NewMessageScreen
import com.gigapingu.neon.feature.notifications.NotificationRequestsScreen
import com.gigapingu.neon.feature.thread.ThreadScreen
import com.gigapingu.neon.feature.timeline.HashtagTimelineScreen
import com.gigapingu.neon.feature.timeline.ListTimelineScreen

// NavDisplay.DEFAULT_TRANSITION_DURATION_MILLISECOND (internal in alpha05).
private const val NAV_TRANSITION_MS = 400

// Fades layered onto the slides mask the transparent-background flash that
// shows through while a new screen's glass surfaces are still compositing in.
private fun pushSlide(): ContentTransform =
    (slideInHorizontally(tween(NAV_TRANSITION_MS)) { it } + fadeIn(tween(NAV_TRANSITION_MS))) togetherWith
        (slideOutHorizontally(tween(NAV_TRANSITION_MS)) { -it / 4 } + fadeOut(tween(NAV_TRANSITION_MS)))

private fun popSlide(): ContentTransform =
    (slideInHorizontally(tween(NAV_TRANSITION_MS)) { -it / 4 } + fadeIn(tween(NAV_TRANSITION_MS))) togetherWith
        (slideOutHorizontally(tween(NAV_TRANSITION_MS)) { it } + fadeOut(tween(NAV_TRANSITION_MS)))

// Composer opens by sliding up from the bottom of the screen, and slides back
// down on pop. The near-1f fades keep the screen underneath composed and
// still for the whole transform (ExitTransition.KeepUntilTransitionsFinished
// is internal).
private fun composerEnter(): ContentTransform =
    (slideInVertically(tween(NAV_TRANSITION_MS)) { it } + fadeIn(tween(NAV_TRANSITION_MS))) togetherWith
        fadeOut(tween(NAV_TRANSITION_MS), targetAlpha = 0.999f)

private fun composerExit(): ContentTransform =
    fadeIn(tween(NAV_TRANSITION_MS), initialAlpha = 0.999f) togetherWith
        (slideOutVertically(tween(NAV_TRANSITION_MS)) { it } + fadeOut(tween(NAV_TRANSITION_MS)))

/**
 * Routes between login and the main shell based on auth state (Flutter's
 * _AuthGate). Hosts the single, app-wide [NeonBackground] — every screen
 * beneath (auth gate, login, and the whole authenticated nav stack) renders
 * on top of it rather than each drawing its own.
 */
@Composable
fun NeonApp(viewModel: ShellViewModel, modifier: Modifier = Modifier) {
    val authStatus by viewModel.authStatus.collectAsStateWithLifecycle()
    NeonBackground(modifier = modifier.fillMaxSize()) {
        Crossfade(targetState = authStatus, label = "authGate") { status ->
            when (status) {
                AuthStatus.Unknown -> Box(Modifier.fillMaxSize()) {
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

                AuthStatus.Authenticated -> AuthenticatedApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun AuthenticatedApp(viewModel: ShellViewModel) {
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
            modifier = Modifier.fillMaxSize(),
            onBack = { count -> repeat(count) { backStack.removeLastOrNull() } },
            transitionSpec = { pushSlide() },
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
                        directToHandle = key.directToHandle,
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
                entry<ManageFollowedHashtagsKey> { ManageFollowedHashtagsScreen() }
                entry<ManageListsKey> { ManageListsScreen() }
                entry<ListTimelineKey> { key -> ListTimelineScreen(listId = key.listId, title = key.title) }
                entry<FiltersKey> { FiltersScreen() }
                entry<NotificationRequestsKey> { NotificationRequestsScreen() }
                entry<BlockedAccountsKey> { BlockedAccountsScreen() }
                entry<BookmarksKey> { BookmarksScreen() }
                entry<NewMessageKey> { NewMessageScreen() }
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
