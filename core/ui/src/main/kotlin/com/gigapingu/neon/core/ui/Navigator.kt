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
data class ComposeKey(val replyToId: String? = null, val quotingId: String? = null) : NavKey

@Serializable
data class FollowListKey(val accountId: String, val handle: String, val following: Boolean) : NavKey

@Serializable
data object EditProfileKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data class MediaPreviewKey(val url: String, val previewUrl: String? = null) : NavKey

/**
 * Global navigation. NeonApp sets [backStack] while the authenticated shell is
 * on screen; while it is null (previews, login) every call is a no-op.
 */
object Navigator {
    var backStack: NavBackStack? = null

    fun openThread(statusId: String) {
        backStack?.add(ThreadKey(statusId))
    }

    fun openProfile(accountId: String) {
        backStack?.add(ProfileKey(accountId))
    }

    fun openHashtag(tag: String) {
        backStack?.add(HashtagKey("#$tag"))
    }

    fun openCompose(replyToId: String? = null, quotingId: String? = null) {
        backStack?.add(ComposeKey(replyToId = replyToId, quotingId = quotingId))
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
    fun openMediaPreview(url: String, previewUrl: String? = null) {
        backStack?.add(MediaPreviewKey(url, previewUrl))
    }

    fun back() {
        backStack?.let { if (it.size > 1) it.removeLastOrNull() }
    }
}
