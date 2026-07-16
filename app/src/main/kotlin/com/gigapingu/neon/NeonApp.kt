package com.gigapingu.neon

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.gigapingu.neon.core.data.AuthStatus
import com.gigapingu.neon.core.designsystem.component.LocalNavAnimatedVisibilityScope
import com.gigapingu.neon.core.designsystem.component.LocalSharedTransitionScope
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.theme.NeonMotion
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.model.Poll
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.ui.LocalNeonNavigator
import com.gigapingu.neon.core.ui.LocalStatusActionHandler
import com.gigapingu.neon.core.ui.NeonNavigator
import com.gigapingu.neon.core.ui.StatusActionHandler
import com.gigapingu.neon.core.ui.media.MediaPreviewScreen
import com.gigapingu.neon.feature.auth.LoginScreen
import com.gigapingu.neon.feature.composer.ComposeScreen
import com.gigapingu.neon.feature.explore.ExploreScreen
import com.gigapingu.neon.feature.profile.EditProfileScreen
import com.gigapingu.neon.feature.profile.FollowListScreen
import com.gigapingu.neon.feature.profile.ProfileScreen
import com.gigapingu.neon.feature.settings.SettingsScreen
import com.gigapingu.neon.feature.thread.ThreadScreen
import com.gigapingu.neon.navigation.ComposeKey
import com.gigapingu.neon.navigation.EditProfileKey
import com.gigapingu.neon.navigation.FollowListKey
import com.gigapingu.neon.navigation.HashtagKey
import com.gigapingu.neon.navigation.HomeKey
import com.gigapingu.neon.navigation.MediaPreviewKey
import com.gigapingu.neon.navigation.ProfileKey
import com.gigapingu.neon.navigation.SettingsKey
import com.gigapingu.neon.navigation.ThreadKey

/** Routes between login and the main shell based on auth state (Flutter's _AuthGate). */
@Composable
fun NeonApp(viewModel: ShellViewModel, modifier: Modifier = Modifier) {
    val authStatus by viewModel.authStatus.collectAsStateWithLifecycle()
    Crossfade(
        targetState = authStatus,
        animationSpec = NeonMotion.screen(),
        label = "authGate",
    ) { status ->
        when (status) {
            AuthStatus.Unknown -> NeonBackground(modifier = modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    color = NeonTheme.palette.cyan,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            AuthStatus.Unauthenticated -> LoginScreen()

            AuthStatus.Authenticated -> AuthenticatedApp(viewModel = viewModel, modifier = modifier)
        }
    }
}

/** Push: new screen slides in from the right, the old one recedes with parallax. */
private val neonPush: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
    ContentTransform(
        slideInHorizontally(NeonMotion.screen()) { it },
        slideOutHorizontally(NeonMotion.screen()) { -it / 4 } +
            fadeOut(NeonMotion.screen(), targetAlpha = .5f),
    )
}

/** Pop mirrors the push. */
private val neonPop: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
    ContentTransform(
        slideInHorizontally(NeonMotion.screen()) { -it / 4 } +
            fadeIn(NeonMotion.screen(), initialAlpha = .5f),
        slideOutHorizontally(NeonMotion.screen()) { it },
    )
}

/** The composer is a modal: it slides up over the scene, which recedes slightly. */
private val composerMetadata =
    NavDisplay.transitionSpec {
        ContentTransform(
            slideInVertically(NeonMotion.screen()) { it },
            scaleOut(NeonMotion.screen(), targetScale = .95f) +
                fadeOut(NeonMotion.screen(), targetAlpha = .6f),
        )
    } + NavDisplay.popTransitionSpec {
        ContentTransform(
            scaleIn(NeonMotion.screen(), initialScale = .95f) +
                fadeIn(NeonMotion.screen(), initialAlpha = .6f),
            slideOutVertically(NeonMotion.screen()) { it },
        )
    } + NavDisplay.predictivePopTransitionSpec {
        ContentTransform(
            scaleIn(NeonMotion.screen(), initialScale = .95f) +
                fadeIn(NeonMotion.screen(), initialAlpha = .6f),
            slideOutVertically(NeonMotion.screen()) { it },
        )
    }

/**
 * The media viewer fades in over the still-visible scene so the shared-element
 * image morph carries the motion; the scene underneath is kept as-is.
 */
@OptIn(ExperimentalAnimationApi::class)
private val mediaPreviewMetadata =
    NavDisplay.transitionSpec {
        ContentTransform(
            fadeIn(NeonMotion.screen()),
            ExitTransition.KeepUntilTransitionsFinished,
        )
    } + NavDisplay.popTransitionSpec {
        ContentTransform(
            EnterTransition.None,
            fadeOut(NeonMotion.screen()),
        )
    } + NavDisplay.predictivePopTransitionSpec {
        ContentTransform(
            EnterTransition.None,
            fadeOut(NeonMotion.screen()),
        )
    }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AuthenticatedApp(viewModel: ShellViewModel, modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(HomeKey)
    val snackbarHostState = remember { SnackbarHostState() }
    val navigator = remember(backStack) { BackStackNavigator(backStack) }
    val actionHandler = remember(viewModel) {
        object : StatusActionHandler {
            override fun toggleFavourite(status: Status) = viewModel.toggleFavourite(status)
            override fun toggleBoost(status: Status) = viewModel.toggleBoost(status)
            override fun vote(poll: Poll, choices: List<Int>) = viewModel.vote(poll, choices)
            override fun share(status: Status) = viewModel.share(status)
            override fun openMention(status: Status, acctOrUrl: String) =
                viewModel.openMention(status, acctOrUrl)
        }
    }

    LaunchedEffect(viewModel, backStack) {
        viewModel.events.collect { event ->
            when (event) {
                is ShellEvent.OpenProfile -> navigator.openProfile(event.accountId)
                is ShellEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    CompositionLocalProvider(
        LocalNeonNavigator provides navigator,
        LocalStatusActionHandler provides actionHandler,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            SharedTransitionLayout {
                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { count -> repeat(count) { backStack.removeLastOrNull() } },
                        entryDecorators = listOf(
                            rememberSceneSetupNavEntryDecorator(),
                            rememberSavedStateNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        transitionSpec = neonPush,
                        popTransitionSpec = neonPop,
                        entryProvider = entryProvider {
                            entry<HomeKey> {
                                HeroScope { HomeShell(viewModel = viewModel) }
                            }
                            entry<ThreadKey> { key ->
                                HeroScope { ThreadScreen(statusId = key.statusId) }
                            }
                            entry<ProfileKey> { key ->
                                HeroScope {
                                    ProfileScreen(accountId = key.accountId, heroKey = key.heroKey)
                                }
                            }
                            entry<HashtagKey> { key ->
                                HeroScope {
                                    ExploreScreen(
                                        initialQuery = key.query,
                                        snackbarHostState = snackbarHostState,
                                    )
                                }
                            }
                            entry<ComposeKey>(metadata = composerMetadata) { key ->
                                ComposeScreen(replyToId = key.replyToId, quotingId = key.quotingId)
                            }
                            entry<FollowListKey> { key ->
                                HeroScope {
                                    FollowListScreen(
                                        accountId = key.accountId,
                                        handle = key.handle,
                                        following = key.following,
                                    )
                                }
                            }
                            entry<EditProfileKey> { EditProfileScreen() }
                            entry<SettingsKey> { SettingsScreen() }
                            entry<MediaPreviewKey>(metadata = mediaPreviewMetadata) { key ->
                                HeroScope {
                                    MediaPreviewScreen(url = key.url, previewUrl = key.previewUrl)
                                }
                            }
                        },
                    )
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * Rebinds Nav3's per-entry AnimatedContent scope to the design-system local so
 * core components can participate in shared-element (hero) transitions without
 * depending on Navigation 3.
 */
@Composable
private fun HeroScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNavAnimatedVisibilityScope provides LocalNavAnimatedContentScope.current,
    ) {
        content()
    }
}

/** NeonNavigator backed by the Navigation 3 back stack. */
private class BackStackNavigator(private val backStack: NavBackStack) : NeonNavigator {
    override fun openThread(statusId: String) {
        backStack.add(ThreadKey(statusId))
    }

    override fun openProfile(accountId: String, heroKey: String?) {
        backStack.add(ProfileKey(accountId, heroKey))
    }

    override fun openHashtag(tag: String) {
        backStack.add(HashtagKey("#$tag"))
    }

    override fun openCompose(replyToId: String?, quotingId: String?) {
        backStack.add(ComposeKey(replyToId = replyToId, quotingId = quotingId))
    }

    override fun openFollowList(accountId: String, handle: String, following: Boolean) {
        backStack.add(FollowListKey(accountId, handle, following))
    }

    override fun openEditProfile() {
        backStack.add(EditProfileKey)
    }

    override fun openSettings() {
        backStack.add(SettingsKey)
    }

    override fun openMediaPreview(url: String, previewUrl: String?) {
        backStack.add(MediaPreviewKey(url, previewUrl))
    }

    override fun back() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }
}
