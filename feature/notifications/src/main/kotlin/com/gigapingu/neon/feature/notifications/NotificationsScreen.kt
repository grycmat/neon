package com.gigapingu.neon.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.PersonAddAlt
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.NeonAvatar
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.htmlToPlainText
import com.gigapingu.neon.core.designsystem.util.relativeTime
import com.gigapingu.neon.core.model.MastoNotification
import com.gigapingu.neon.core.model.NotificationType
import com.gigapingu.neon.core.ui.AsyncList
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.LocalShellPadding
import com.gigapingu.neon.core.ui.PreviewFixtures
import com.gigapingu.neon.core.ui.PreviewHarness
import java.time.Instant

/** Notifications feed — favourite / boost / follow / mention / poll rows. */
@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel = hiltViewModel()) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shellPadding = LocalShellPadding.current

    NeonBackground {
        Column(Modifier.fillMaxSize()) {
            AsyncList(
                state = state,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadMore,
                emptyLabel = "All quiet — for now.",
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 8.dp + shellPadding.calculateTopPadding(),
                    end = 16.dp,
                    bottom = 90.dp + shellPadding.calculateBottomPadding(),
                ),
                key = { it.id },
            ) { notification ->
                NotificationRow(item = notification)
            }
        }
    }
}

@Composable
private fun NotificationRow(item: MastoNotification) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type

    val (icon, color, verb) = when (item.type) {
        NotificationType.Favourite -> Triple(Icons.Rounded.Star, palette.pink, "favourited your toot")
        NotificationType.Reblog -> Triple(Icons.Rounded.Repeat, palette.cyan, "boosted your toot")
        NotificationType.Quote -> Triple(Icons.Rounded.FormatQuote, palette.purple, "quoted your toot")
        NotificationType.Follow -> Triple(Icons.Rounded.PersonAddAlt, palette.pink, "followed you")
        NotificationType.FollowRequest ->
            Triple(Icons.Rounded.PersonAddAlt, palette.purple, "requested to follow you")
        NotificationType.Mention -> Triple(Icons.Rounded.AlternateEmail, palette.purple, "mentioned you")
        NotificationType.Poll -> Triple(Icons.Rounded.Poll, palette.cyan, "a poll you voted in has ended")
        NotificationType.Update -> Triple(Icons.Rounded.Edit, palette.textDim, "edited a toot")
        else -> Triple(Icons.Rounded.Bolt, palette.textDim, "did something")
    }

    val preview = item.status
        ?.let { htmlToPlainText(it.content).replace('\n', ' ').trim() }
        .orEmpty()

    GlassCard(
        modifier = Modifier.padding(vertical = 5.dp),
        contentPadding = PaddingValues(14.dp),
        onClick = {
            val status = item.status
            if (status != null) {
                Navigator.openThread(status.display.id)
            } else {
                Navigator.openProfile(item.account.id)
            }
        },
    ) {
        Row {
            Box {
                NeonAvatar(account = item.account, size = 40.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(palette.surfaceSolid)
                        .border(1.dp, color.copy(alpha = .5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    buildAnnotatedString {
                        withStyle(type.titleSmall.copy(color = palette.text).toSpanStyle()) {
                            append(item.account.displayNameOrUsername)
                        }
                        withStyle(type.bodyMedium.copy(color = palette.textDim).toSpanStyle()) {
                            append(" $verb")
                        }
                    },
                    style = type.bodyMedium,
                )
                if (preview.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        preview,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = type.bodySmall,
                        color = palette.textDim,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                relativeTime(item.createdAt),
                style = type.bodySmall,
                color = palette.textMute,
            )
        }
    }
}

private fun previewNotification(id: String, type: String, withStatus: Boolean = true) = MastoNotification(
    id = id,
    rawType = type,
    createdAt = Instant.now().minusSeconds(60 * 12),
    account = PreviewFixtures.account2,
    status = if (withStatus) PreviewFixtures.status else null,
)

@Preview(name = "Notification rows", showBackground = true, heightDp = 560)
@Composable
private fun NotificationRowPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            NotificationRow(item = previewNotification("1", "favourite"))
            NotificationRow(item = previewNotification("2", "reblog"))
            NotificationRow(item = previewNotification("3", "mention"))
            NotificationRow(item = previewNotification("4", "follow", withStatus = false))
            NotificationRow(item = previewNotification("5", "poll"))
        }
    }
}
