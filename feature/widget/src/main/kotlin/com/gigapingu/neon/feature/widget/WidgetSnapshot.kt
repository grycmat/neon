package com.gigapingu.neon.feature.widget

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.annotation.DrawableRes
import com.gigapingu.neon.core.designsystem.theme.NeonPalette
import com.gigapingu.neon.core.model.MastoNotification
import com.gigapingu.neon.core.model.NotificationType

/**
 * Everything the widget needs to draw one update, resolved off the composition.
 *
 * Glance runs `provideGlance` as a suspend function and then composes synchronously, so all the
 * blocking work — Room reads, avatar decoding — happens up front and the composable itself is a
 * pure function of this snapshot.
 */
internal data class WidgetSnapshot(
    val palette: NeonPalette,
    val content: WidgetContent,
    /** "Updated 4m ago", or empty before the first successful fetch. */
    val updatedLabel: String,
)

internal sealed interface WidgetContent {
    /** No stored session — the widget can't show anything until the user signs in. */
    data object SignedOut : WidgetContent

    /** Widget just added, nothing fetched or cached yet. */
    data object Loading : WidgetContent

    data class Ready(val rows: List<WidgetRow>) : WidgetContent
}

internal data class WidgetRow(
    val id: String,
    val name: String,
    val verb: String,
    /** Plain-text preview of the toot, empty for account-only notifications (follows). */
    val preview: String,
    val time: String,
    /** Thread to deep-link to, null when the notification has no status (follow / follow request). */
    val statusId: String?,
    /** Avatar with the type badge already composited in — see [WidgetAvatars]. */
    val avatar: Bitmap?,
)

/** The badge drawn on the avatar and the accent it is tinted with, mirroring NotificationsScreen. */
internal data class NotificationLook(
    @DrawableRes val icon: Int,
    val accent: Color,
    val verb: String,
)

internal fun MastoNotification.look(palette: NeonPalette): NotificationLook = when (type) {
    NotificationType.Favourite ->
        NotificationLook(R.drawable.ic_widget_favourite, palette.pink, "favourited your toot")
    NotificationType.Reblog ->
        NotificationLook(R.drawable.ic_widget_boost, palette.cyan, "boosted your toot")
    NotificationType.Quote ->
        NotificationLook(R.drawable.ic_widget_quote, palette.purple, "quoted your toot")
    NotificationType.Follow ->
        NotificationLook(R.drawable.ic_widget_follow, palette.pink, "followed you")
    NotificationType.FollowRequest ->
        NotificationLook(R.drawable.ic_widget_follow, palette.purple, "requested to follow you")
    NotificationType.Mention ->
        NotificationLook(R.drawable.ic_widget_mention, palette.purple, "mentioned you")
    NotificationType.Poll ->
        NotificationLook(R.drawable.ic_widget_poll, palette.cyan, "a poll you voted in has ended")
    NotificationType.Update ->
        NotificationLook(R.drawable.ic_widget_edit, palette.textDim, "edited a toot")
    else ->
        NotificationLook(R.drawable.ic_widget_bolt, palette.textDim, "did something")
}
