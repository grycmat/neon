package com.gigapingu.neon

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
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.gigapingu.neon.core.data.AuthStatus
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.model.Poll
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.ui.LocalNeonNavigator
import com.gigapingu.neon.core.ui.LocalStatusActionHandler
import com.gigapingu.neon.core.ui.NeonNavigator
import com.gigapingu.neon.core.ui.StatusActionHandler
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
import com.gigapingu.neon.navigation.ProfileKey
import com.gigapingu.neon.navigation.SettingsKey
import com.gigapingu.neon.navigation.ThreadKey

/** Routes between login and the main shell based on auth state (Flutter's _AuthGate). */
@Composable
fun NeonApp(viewModel: ShellViewModel, modifier: Modifier = Modifier) {
    val authStatus by viewModel.authStatus.collectAsStateWithLifecycle()
    when (authStatus) {
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
            NavDisplay(
                backStack = backStack,
                onBack = { count -> repeat(count) { backStack.removeLastOrNull() } },
                entryDecorators = listOf(
                    rememberSceneSetupNavEntryDecorator(),
                    rememberSavedStateNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<HomeKey> { HomeShell(viewModel = viewModel) }
                    entry<ThreadKey> { key -> ThreadScreen(statusId = key.statusId) }
                    entry<ProfileKey> { key -> ProfileScreen(accountId = key.accountId) }
                    entry<HashtagKey> { key ->
                        ExploreScreen(initialQuery = key.query, snackbarHostState = snackbarHostState)
                    }
                    entry<ComposeKey> { key ->
                        ComposeScreen(replyToId = key.replyToId, quotingId = key.quotingId)
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
                },
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** NeonNavigator backed by the Navigation 3 back stack. */
private class BackStackNavigator(private val backStack: NavBackStack) : NeonNavigator {
    override fun openThread(statusId: String) {
        backStack.add(ThreadKey(statusId))
    }

    override fun openProfile(accountId: String) {
        backStack.add(ProfileKey(accountId))
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

    override fun back() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }
}
