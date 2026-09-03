package com.gigapingu.neon.core.ui

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.gigapingu.neon.core.model.MediaAttachment
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
    val directToHandle: String? = null,
    val sharedText: String? = null,
    val sharedMediaUris: List<String>? = null,
) : NavKey

@Serializable
data object BookmarksKey : NavKey

@Serializable
data object NewMessageKey : NavKey

@Serializable
data class FollowListKey(val accountId: String, val handle: String, val following: Boolean) : NavKey

@Serializable
data object EditProfileKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data object ManageFollowedHashtagsKey : NavKey

@Serializable
data class MediaPreviewKey(val attachments: List<MediaAttachment>, val startIndex: Int = 0) : NavKey

@Serializable
data class HashtagTimelineKey(val hashtag: String) : NavKey

@Serializable
data object ManageListsKey : NavKey

@Serializable
data class ListTimelineKey(val listId: String, val title: String) : NavKey

@Serializable
data object FiltersKey : NavKey

@Serializable
data object NotificationRequestsKey : NavKey

@Serializable
data object FollowRequestsKey : NavKey

@Serializable
data object BlockedAccountsKey : NavKey

/**
 * Global navigation. NeonApp sets [backStack] while the authenticated shell is
 * on screen; while it is null (previews, login) every call is a no-op.
 */
object Navigator {
    var backStack: NavBackStack? = null

    private var onPendingNotification: ((String?, Boolean) -> Unit)? = null
    private var pendingStatusId: String? = null
    private var pendingOpenNotifications: Boolean = false

    fun handleNotificationClick(statusId: String?, openNotificationsTab: Boolean) {
        val handler = onPendingNotification
        if (handler != null) {
            handler(statusId, openNotificationsTab)
        } else {
            pendingStatusId = statusId
            pendingOpenNotifications = openNotificationsTab
        }
    }

    fun bindNotificationHandler(handler: (String?, Boolean) -> Unit) {
        onPendingNotification = handler
        if (pendingStatusId != null || pendingOpenNotifications) {
            handler(pendingStatusId, pendingOpenNotifications)
            pendingStatusId = null
            pendingOpenNotifications = false
        }
    }

    fun unbindNotificationHandler() {
        onPendingNotification = null
    }

    private var onPendingShare: ((String?, List<String>) -> Unit)? = null
    private var pendingSharedText: String? = null
    private var pendingSharedMediaUris: List<String> = emptyList()

    /** Called from MainActivity when an ACTION_SEND/ACTION_SEND_MULTIPLE Intent arrives. */
    fun handleShare(text: String?, mediaUris: List<String>) {
        val handler = onPendingShare
        if (handler != null) {
            handler(text, mediaUris)
        } else {
            pendingSharedText = text
            pendingSharedMediaUris = mediaUris
        }
    }

    fun bindShareHandler(handler: (String?, List<String>) -> Unit) {
        onPendingShare = handler
        if (pendingSharedText != null || pendingSharedMediaUris.isNotEmpty()) {
            handler(pendingSharedText, pendingSharedMediaUris)
            pendingSharedText = null
            pendingSharedMediaUris = emptyList()
        }
    }

    fun unbindShareHandler() {
        onPendingShare = null
    }

    /**
     * Big-screen HomeShell binds this while it is on screen: when it returns
     * true the thread was shown in the shell's detail pane and nothing is
     * pushed. Null (phones, pushed screens) means every thread push navigates.
     */
    var threadPaneHandler: ((String) -> Boolean)? = null

    /** Set by the currently-active Home tab instance; null elsewhere/no-op. */
    var scrollToTopHandler: (() -> Unit)? = null

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

    /** Tapped from inline status/bio text: Explore's search, prepopulated and
     *  run, same as tapping a trending tag — not the dedicated hashtag timeline. */
    fun openHashtagSearch(tag: String) {
        backStack?.add(HashtagKey("#$tag"))
    }

    fun openCompose(
        replyToId: String? = null,
        quotingId: String? = null,
        editStatusId: String? = null,
        redraftText: String? = null,
        redraftSpoilerText: String? = null,
        redraftVisibility: String? = null,
        directToHandle: String? = null,
        sharedText: String? = null,
        sharedMediaUris: List<String>? = null,
    ) {
        backStack?.add(
            ComposeKey(
                replyToId = replyToId,
                quotingId = quotingId,
                editStatusId = editStatusId,
                redraftText = redraftText,
                redraftSpoilerText = redraftSpoilerText,
                redraftVisibility = redraftVisibility,
                directToHandle = directToHandle,
                sharedText = sharedText,
                sharedMediaUris = sharedMediaUris,
            )
        )
    }

    fun openBookmarks() {
        backStack?.add(BookmarksKey)
    }

    fun openNewMessage() {
        backStack?.add(NewMessageKey)
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

    fun openManageFollowedHashtags() {
        backStack?.add(ManageFollowedHashtagsKey)
    }

    fun openManageLists() {
        backStack?.add(ManageListsKey)
    }

    fun openListTimeline(listId: String, title: String) {
        backStack?.add(ListTimelineKey(listId, title))
    }

    fun openFilters() {
        backStack?.add(FiltersKey)
    }

    fun openNotificationRequests() {
        backStack?.add(NotificationRequestsKey)
    }

    fun openFollowRequests() {
        backStack?.add(FollowRequestsKey)
    }

    fun openBlockedAccounts() {
        backStack?.add(BlockedAccountsKey)
    }

    /** [startIndex] is which of [attachments] to open on, for a status with multiple media items. */
    fun openMediaPreview(attachments: List<MediaAttachment>, startIndex: Int = 0) {
        if (attachments.isEmpty()) return
        backStack?.add(MediaPreviewKey(attachments, startIndex.coerceIn(0, attachments.size - 1)))
    }

    fun back() {
        backStack?.let { if (it.size > 1) it.removeLastOrNull() }
    }
}
