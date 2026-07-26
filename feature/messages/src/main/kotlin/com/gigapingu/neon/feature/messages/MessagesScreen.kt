package com.gigapingu.neon.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.NeonAvatar
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.htmlToPlainText
import com.gigapingu.neon.core.designsystem.util.relativeTime
import com.gigapingu.neon.core.model.Conversation
import com.gigapingu.neon.core.ui.AsyncList
import com.gigapingu.neon.core.ui.LocalShellPadding
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.PaneSelection
import com.gigapingu.neon.core.ui.PreviewFixtures
import com.gigapingu.neon.core.ui.PreviewHarness

/**
 * Direct-message conversations. Rows open the underlying [ThreadScreen] —
 * Mastodon conversations are just threads containing a direct-visibility
 * status, so there is no separate detail screen. [selectedStatusId] marks
 * the conversation open in the big-screen detail pane.
 */
@Composable
fun MessagesScreen(
    selectedStatusId: String? = null,
    viewModel: MessagesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shellPadding = LocalShellPadding.current

    Column(Modifier.fillMaxSize()) {
        AsyncList(
            state = state,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            emptyLabel = "No messages yet.",
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp + shellPadding.calculateTopPadding(),
                end = 16.dp,
                bottom = 90.dp + shellPadding.calculateBottomPadding(),
            ),
            key = { it.id },
        ) { conversation ->
            PaneSelection(
                selected = selectedStatusId != null &&
                    conversation.lastStatus?.display?.id == selectedStatusId,
            ) {
                ConversationRow(
                    conversation = conversation,
                    onOpen = {
                        conversation.lastStatus?.let { last ->
                            if (conversation.unread) viewModel.markRead(conversation.id)
                            Navigator.openThread(last.display.id)
                        }
                    },
                    onDelete = { viewModel.delete(conversation.id) },
                )
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val participant = conversation.accounts.firstOrNull()
    val last = conversation.lastStatus
    val preview = remember(last?.content) {
        last?.content?.let { htmlToPlainText(it).replace('\n', ' ').trim() }.orEmpty()
    }

    GlassCard(
        modifier = Modifier.padding(vertical = 5.dp),
        contentPadding = PaddingValues(14.dp),
        onClick = onOpen,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                NeonAvatar(account = participant, size = 42.dp)
                if (conversation.unread) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(palette.pink),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    participant?.displayNameOrUsername ?: "Unknown",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = type.titleSmall.copy(
                        fontWeight = if (conversation.unread) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = palette.text,
                )
                if (preview.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        preview,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = type.bodySmall,
                        color = if (conversation.unread) palette.text else palette.textDim,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    relativeTime(last?.createdAt),
                    style = type.bodySmall,
                    color = palette.textMute,
                )
                Spacer(Modifier.height(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Delete conversation",
                        tint = palette.textMute,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Preview(name = "Conversation rows", showBackground = true, heightDp = 300)
@Composable
private fun ConversationRowPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            PreviewFixtures.conversations.forEach { conversation ->
                ConversationRow(conversation = conversation, onOpen = {}, onDelete = {})
            }
        }
    }
}
