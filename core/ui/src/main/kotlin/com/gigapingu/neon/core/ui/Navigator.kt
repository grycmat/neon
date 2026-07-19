package com.gigapingu.neon.core.ui

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 keys. Screens receive ids (not whole entities) so the back
 * stack stays small and process-death-safe; each screen loads its own data
 * (cache-first where the Flutter app did).
 */
@Serializable
data object HomeKey : NavKey

@Serializable
data class ThreadKey(val statusId: String) : NavKey

@Serializable
data class ProfileKey(val accountId: String) : NavKey

@Serializable
data class HashtagKey(val query: String) : NavKey

@Serializable
data class ComposeKey(
    val replyToId: String? = null,
    val quotingId: String? = null,
    val editStatusId: String? = null,
    val redraftText: String? = null,
    val redraftSpoilerText: String? = null,
    val redraftVisibility: String? = null,
) : NavKey

@Serializable
data object BookmarksKey : NavKey

@Serializable
data class FollowListKey(val accountId: String, val handle: String, val following: Boolean) : NavKey

@Serializable
data object EditProfileKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data class MediaPreviewKey(val url: String, val previewUrl: String? = null, val type: String? = null) : NavKey

@Serializable
data class HashtagTimelineKey(val hashtag: String) : NavKey

/**
 * Global navigation. NeonApp sets [backStack] while the authenticated shell is
 * on screen; while it is null (previews, login) every call is a no-op.
 */
object Navigator {
    var backStack: NavBackStack? = null

    /**
     * Big-screen HomeShell binds this while it is on screen: when it returns
     * true the thread was shown in the shell's detail pane and nothing is
     * pushed. Null (phones, pushed screens) means every thread push navigates.
     */
    var threadPaneHandler: ((String) -> Boolean)? = null

    fun openThread(statusId: String) {
        if (threadPaneHandler?.invoke(statusId) == true) return
        backStack?.add(ThreadKey(statusId))
    }

    fun openProfile(accountId: String) {
        backStack?.add(ProfileKey(accountId))
    }

    fun openHashtag(tag: String) {
        backStack?.add(HashtagTimelineKey(tag))
    }

    fun openCompose(
        replyToId: String? = null,
        quotingId: String? = null,
        editStatusId: String? = null,
        redraftText: String? = null,
        redraftSpoilerText: String? = null,
        redraftVisibility: String? = null,
    ) {
        backStack?.add(
            ComposeKey(
                replyToId = replyToId,
                quotingId = quotingId,
                editStatusId = editStatusId,
                redraftText = redraftText,
                redraftSpoilerText = redraftSpoilerText,
                redraftVisibility = redraftVisibility,
            )
        )
    }

    fun openBookmarks() {
        backStack?.add(BookmarksKey)
    }

    fun openFollowList(accountId: String, handle: String, following: Boolean) {
        backStack?.add(FollowListKey(accountId, handle, following))
    }

    fun openEditProfile() {
        backStack?.add(EditProfileKey)
    }

    fun openSettings() {
        backStack?.add(SettingsKey)
    }

    /** [previewUrl] is the already-cached thumbnail shown while [url] loads. */
    fun openMediaPreview(url: String, previewUrl: String? = null, type: String? = null) {
        backStack?.add(MediaPreviewKey(url, previewUrl, type))
    }

    fun back() {
        backStack?.let { if (it.size > 1) it.removeLastOrNull() }
    }
}
