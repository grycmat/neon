package com.gigapingu.neon.navigation

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
data class MediaPreviewKey(val url: String) : NavKey

