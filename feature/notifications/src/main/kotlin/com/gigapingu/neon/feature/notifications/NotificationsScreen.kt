package com.gigapingu.neon.feature.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.PersonAddAlt
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.data.NotificationKind
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.NeonAvatar
import com.gigapingu.neon.core.designsystem.component.SegmentPill
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.htmlToPlainText
import com.gigapingu.neon.core.designsystem.util.relativeTime
import com.gigapingu.neon.core.model.MastoNotification
import com.gigapingu.neon.core.model.NotificationType
import com.gigapingu.neon.core.ui.AsyncList
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.LocalShellPadding
import com.gigapingu.neon.core.ui.PaneSelection
import com.gigapingu.neon.core.ui.PreviewFixtures
import com.gigapingu.neon.core.ui.PreviewHarness
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant

/**
 * Notifications feed — favourite / boost / follow / mention / poll rows.
 * [selectedStatusId] marks the toot open in the big-screen detail pane.
 */
@Composable
fun NotificationsScreen(
    selectedStatusId: String? = null,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val kind by viewModel.kind.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val requestsCount by viewModel.requestsCount.collectAsStateWithLifecycle()
    val followRequestsCount by viewModel.followRequestsCount.collectAsStateWithLifecycle()
    val shellPadding = LocalShellPadding.current
    val listState = rememberLazyListState()

    // Pills float over the list, same as TimelineScreen's Home/Local/Federated row.
    var pillsHeightPx by remember { mutableIntStateOf(0) }
    val pillsHeight = with(LocalDensity.current) { pillsHeightPx.toDp() }

    // Hidden while scrolling down through the list, shown again on scroll-up
    // or once back at the top. Reset per-kind so switching pills always shows it.
    var pillsVisible by remember(kind) { mutableStateOf(true) }
    var lastScrollPosition by remember(kind) { mutableStateOf(0 to 0) }

    LaunchedEffect(listState, kind) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                val isAtTop = index == 0 && offset == 0
                if (isAtTop) {
                    pillsVisible = true
                } else {
                    val (lastIndex, lastOffset) = lastScrollPosition
                    val scrolledDown = index > lastIndex || (index == lastIndex && offset > lastOffset)
                    val scrolledUp = index < lastIndex || (index == lastIndex && offset < lastOffset)
                    if (scrolledDown) pillsVisible = false
                    if (scrolledUp) pillsVisible = true
                }
                lastScrollPosition = index to offset
            }
    }

    Box(Modifier.fillMaxSize()) {
        AsyncList(
            state = state,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            emptyLabel = "All quiet — for now.",
            modifier = Modifier.fillMaxSize(),
            listState = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                top = pillsHeight,
                end = 16.dp,
                bottom = 90.dp + shellPadding.calculateBottomPadding(),
            ),
            key = { it.id },
            contentType = { it.status != null },
            header = {
                if (followRequestsCount > 0) {
                    FollowRequestsBanner(
                        count = followRequestsCount,
                        onClick = Navigator::openFollowRequests,
                    )
                }
                if (requestsCount > 0) {
                    NotificationRequestsBanner(
                        count = requestsCount,
                        onClick = Navigator::openNotificationRequests,
                    )
                }
            },
        ) { notification ->
            PaneSelection(
                selected = selectedStatusId != null &&
                    notification.status?.display?.id == selectedStatusId,
            ) {
                NotificationRow(
                    item = notification,
                    onDismiss = { viewModel.dismiss(notification.id) },
                    onAuthorizeFollow = { viewModel.authorizeFollow(notification) },
                    onRejectFollow = { viewModel.rejectFollow(notification) },
                )
            }
        }
        AnimatedVisibility(
            visible = pillsVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surfaceSolid.copy(alpha = .80f))
                    .onSizeChanged { pillsHeightPx = it.height }
                    .padding(top = shellPadding.calculateTopPadding())
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NotificationKind.entries.forEach { entry ->
                    SegmentPill(
                        label = entry.label,
                        active = entry == kind,
                        onClick = { viewModel.switchTo(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRequestsBanner(count: Int, onClick: () -> Unit) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    GlassCard(
        modifier = Modifier.padding(bottom = 8.dp),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.PersonAddAlt, contentDescription = null, tint = palette.purple, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Filtered notifications from $count ${if (count == 1) "person" else "people"}",
                style = type.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.text,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FollowRequestsBanner(count: Int, onClick: () -> Unit) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    GlassCard(
        modifier = Modifier.padding(bottom = 8.dp),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.PersonAddAlt, contentDescription = null, tint = palette.purple, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "$count follow ${if (count == 1) "request" else "requests"} to review",
                style = type.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.text,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NotificationRow(
    item: MastoNotification,
    onDismiss: () -> Unit,
    onAuthorizeFollow: () -> Unit = {},
    onRejectFollow: () -> Unit = {},
) {
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

    val statusContent = item.status?.content
    val preview = remember(statusContent) {
        statusContent?.let { htmlToPlainText(it).replace('\n', ' ').trim() }.orEmpty()
    }

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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (preview.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        preview,
                        maxLines = if (item.type == NotificationType.Mention) 10 else 2,
                        overflow = TextOverflow.Ellipsis,
                        style = type.bodySmall,
                        color = palette.textDim,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    relativeTime(item.createdAt),
                    style = type.bodySmall,
                    color = palette.textMute,
                )
                Spacer(Modifier.height(4.dp))
                if (item.type == NotificationType.FollowRequest) {
                    Row {
                        androidx.compose.material3.IconButton(
                            onClick = onRejectFollow,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Reject follow request",
                                tint = palette.textMute,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        androidx.compose.material3.IconButton(
                            onClick = onAuthorizeFollow,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Authorize follow request",
                                tint = palette.purple,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    androidx.compose.material3.IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss notification",
                            tint = palette.textMute,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
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
            NotificationRow(item = previewNotification("1", "favourite"), onDismiss = {})
            NotificationRow(item = previewNotification("2", "reblog"), onDismiss = {})
            NotificationRow(item = previewNotification("3", "mention"), onDismiss = {})
            NotificationRow(item = previewNotification("4", "follow", withStatus = false), onDismiss = {})
            NotificationRow(item = previewNotification("5", "poll"), onDismiss = {})
            NotificationRow(item = previewNotification("6", "follow_request", withStatus = false), onDismiss = {})
        }
    }
}

@Preview(name = "Notification filter pills", showBackground = true, heightDp = 100)
@Composable
private fun NotificationPillsPreview() {
    PreviewHarness {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NotificationKind.entries.forEachIndexed { index, entry ->
                SegmentPill(label = entry.label, active = index == 0) {}
            }
        }
    }
}
