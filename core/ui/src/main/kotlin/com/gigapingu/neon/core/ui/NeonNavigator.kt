package com.gigapingu.neon.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.gigapingu.neon.core.model.Poll
import com.gigapingu.neon.core.model.Status

/**
 * Cross-feature navigation, provided by the app shell as a CompositionLocal
 * so deeply nested components (status cards, html spans) can navigate without
 * threading callbacks through every layer.
 */
interface NeonNavigator {
    fun openThread(statusId: String)
    fun openProfile(accountId: String)
    fun openHashtag(tag: String)
    fun openCompose(replyToId: String? = null, quotingId: String? = null)
    fun openFollowList(accountId: String, handle: String, following: Boolean)
    fun openEditProfile()
    fun openSettings()
    fun back()
}

val LocalNeonNavigator = staticCompositionLocalOf<NeonNavigator> {
    error("NeonNavigator not provided")
}

/**
 * Status interactions (favourite / boost / quote / vote / share), provided by
 * the app shell. Implementations run the network call, broadcast the update
 * through StatusRepository, and surface failures on the shared snackbar.
 */
interface StatusActionHandler {
    fun toggleFavourite(status: Status)
    fun toggleBoost(status: Status)
    fun vote(poll: Poll, choices: List<Int>)
    fun share(status: Status)
    /** Resolves a tapped mention to an account and opens its profile. */
    fun openMention(status: Status, acctOrUrl: String)
}

val LocalStatusActionHandler = staticCompositionLocalOf<StatusActionHandler> {
    error("StatusActionHandler not provided")
}
