package com.gigapingu.neon.feature.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.GradientButton
import com.gigapingu.neon.core.designsystem.component.NeonAvatar
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.component.NeonLabel
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.fullTime
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.ui.LocalShellPadding
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.PreviewFixtures
import com.gigapingu.neon.core.ui.PreviewHarness
import com.gigapingu.neon.core.ui.hingePaneWidth
import com.gigapingu.neon.core.ui.isBigScreen
import com.gigapingu.neon.core.ui.status.LinkPreviewCard
import com.gigapingu.neon.core.ui.status.MediaGrid
import com.gigapingu.neon.core.ui.status.PollView
import com.gigapingu.neon.core.ui.status.QuoteCard
import com.gigapingu.neon.core.ui.status.StatusActions
import com.gigapingu.neon.core.ui.status.StatusBody
import com.gigapingu.neon.core.ui.status.StatusCard
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Thread view: ancestors → focused toot (large) → descendants. Three layouts:
 * phone single column, big-screen focus mode (toot left of the hinge, replies
 * right), and [embedded] — a chrome-less pane inside big-screen HomeShell's
 * list-detail tabs.
 */
@Composable
fun ThreadScreen(
    statusId: String,
    embedded: Boolean = false,
    viewModel: ThreadViewModel = hiltViewModel(key = "thread-$statusId"),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(statusId) { viewModel.start(statusId) }

    NeonBackground {
        when {
            embedded -> EmbeddedThread(uiState = uiState, onRefresh = viewModel::refresh)
            isBigScreen() -> TwoPaneThread(uiState = uiState, onRefresh = viewModel::refresh)
            else -> PhoneThread(uiState = uiState, onRefresh = viewModel::refresh)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneThread(uiState: ThreadUiState, onRefresh: () -> Unit) {
    val palette = NeonTheme.palette
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ThreadTopBar()
        val status = uiState.status
        if (status == null && uiState.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = palette.cyan)
            }
            return@Column
        }
        PullToRefreshBox(
            isRefreshing = uiState.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 40.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                focusItems(uiState)
                replyItems(uiState.context?.descendants.orEmpty(), withLabel = true)
            }
        }
    }
}

/** Big-screen focus mode: focused toot left of the hinge, replies right. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwoPaneThread(uiState: ThreadUiState, onRefresh: () -> Unit) {
    val palette = NeonTheme.palette
    Row(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.width(hingePaneWidth()).fillMaxHeight()) {
            ThreadTopBar()
            val status = uiState.status
            if (status == null && uiState.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.cyan)
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = uiState.refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 40.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        focusItems(uiState)
                    }
                }
            }
        }
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(palette.divider),
        )
        Column(Modifier.fillMaxHeight().weight(1f)) {
            val descendants = uiState.context?.descendants.orEmpty()
            NeonLabel(
                "Replies · ${descendants.size}",
                modifier = Modifier.padding(start = 22.dp, top = 16.dp, end = 22.dp, bottom = 2.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 24.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                replyItems(descendants, withLabel = false)
                if (descendants.isEmpty() && !uiState.loading) {
                    item(key = "no-replies") {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 50.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No replies yet",
                                style = NeonTheme.type.bodyMedium,
                                color = palette.textMute,
                            )
                        }
                    }
                }
            }
            uiState.status?.let { ReplyBar(status = it.display) }
        }
    }
}

/** Chrome-less pane for HomeShell's big-screen list-detail tabs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmbeddedThread(uiState: ThreadUiState, onRefresh: () -> Unit) {
    val palette = NeonTheme.palette
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = LocalShellPadding.current.calculateTopPadding()),
    ) {
        NeonLabel(
            "Thread",
            modifier = Modifier.padding(start = 22.dp, top = 16.dp, end = 22.dp, bottom = 2.dp),
        )
        val status = uiState.status
        if (status == null && uiState.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = palette.cyan)
            }
            return@Column
        }
        PullToRefreshBox(
            isRefreshing = uiState.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                focusItems(uiState)
                replyItems(uiState.context?.descendants.orEmpty(), withLabel = true)
            }
        }
        status?.let { ReplyBar(status = it.display) }
    }
}

@Composable
private fun ThreadTopBar() {
    Row(
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
            onClick = Navigator::back,
            contentDescription = "Back",
        )
        Spacer(Modifier.width(10.dp))
        Text("Thread", style = NeonTheme.type.headlineMedium, color = NeonTheme.palette.text)
    }
}

/** Ancestors, the focused toot, and any load error. */
private fun LazyListScope.focusItems(uiState: ThreadUiState) {
    val ancestors = uiState.context?.ancestors.orEmpty()
    if (ancestors.isNotEmpty()) {
        item(key = "in-reply-to-label") {
            NeonLabel(
                "In reply to",
                modifier = Modifier.padding(start = 6.dp, top = 6.dp, end = 6.dp, bottom = 8.dp),
            )
        }
    }
    items(count = ancestors.size, key = { "anc-" + ancestors[it].id }) { index ->
        StatusCard(status = ancestors[index])
    }
    uiState.status?.let { status ->
        item(key = "focused") { FocusedStatus(status = status.display) }
    }
    uiState.error?.let { error ->
        item(key = "error") {
            Text(
                "Could not load thread: $error",
                style = NeonTheme.type.bodySmall,
                color = NeonTheme.palette.textDim,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

private fun LazyListScope.replyItems(descendants: List<Status>, withLabel: Boolean) {
    if (descendants.isEmpty()) return
    if (withLabel) {
        item(key = "replies-label") {
            NeonLabel(
                "Replies",
                modifier = Modifier.padding(start = 6.dp, top = 18.dp, end = 6.dp, bottom = 8.dp),
            )
        }
    }
    items(count = descendants.size, key = { "dec-" + descendants[it].id }) { index ->
        StatusCard(status = descendants[index])
    }
}

/** Pane footer: tap-through reply prompt that opens the composer. */
@Composable
private fun ReplyBar(status: Status) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider),
        )
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val shape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(shape)
                    .background(palette.surface)
                    .border(1.dp, palette.border, shape)
                    .clickable { Navigator.openCompose(replyToId = status.id) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    "Reply to @${status.account.username}…",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = type.bodyMedium,
                    color = palette.textMute,
                )
            }
            Spacer(Modifier.width(10.dp))
            GradientButton(
                label = "Reply",
                height = 36.dp,
                onClick = { Navigator.openCompose(replyToId = status.id) },
            )
        }
    }
}

/** The focused toot — larger type, full timestamp, prominent StatusActionService. */
@Composable
private fun FocusedStatus(status: Status) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        highlighted = true,
        contentPadding = PaddingValues(18.dp),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    interactionSource = null,
                    indication = null,
                ) { Navigator.openProfile(status.account.id) },
            ) {
                NeonAvatar(account = status.account, size = 46.dp, ring = true)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(status.account.displayNameOrUsername, style = type.titleMedium, color = palette.text)
                    Text(status.account.fullHandle, style = type.bodySmall, color = palette.textDim)
                }
            }
            Spacer(Modifier.height(14.dp))
            val hasCw = status.spoilerText.isNotEmpty()
            var revealed by rememberSaveable(status.id) { androidx.compose.runtime.mutableStateOf(false) }

            StatusBody(
                status = status,
                textStyle = type.bodyLarge.copy(fontSize = 16.sp),
                revealed = revealed,
                onToggleReveal = { revealed = !revealed }
            )
            if (!hasCw || revealed) {
                status.card?.let { card ->
                    LinkPreviewCard(card = card)
                }
                status.quote?.let { quoted ->
                    QuoteCard(status = quoted, onClick = { Navigator.openThread(quoted.id) })
                }
                if (status.mediaAttachments.isNotEmpty()) {
                    MediaGrid(attachments = status.mediaAttachments, sensitive = status.sensitive)
                }
                status.poll?.let { PollView(poll = it) }
            }
            Spacer(Modifier.height(12.dp))
            Text(fullTime(status.createdAt), style = type.bodySmall, color = palette.textMute)
            StatusActions(status = status)
        }
    }
}

@Preview(name = "Focused toot", showBackground = true, heightDp = 380)
@Composable
private fun FocusedStatusPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            FocusedStatus(status = PreviewFixtures.status)
        }
    }
}

@Preview(name = "Focused toot — poll", showBackground = true, heightDp = 480)
@Composable
private fun FocusedStatusPollPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            FocusedStatus(
                status = PreviewFixtures.status.copy(
                    content = "<p>Which accent wins?</p>",
                    poll = PreviewFixtures.poll,
                ),
            )
        }
    }
}

@Preview(name = "Reply bar", showBackground = true, heightDp = 120)
@Composable
private fun ReplyBarPreview() {
    PreviewHarness {
        Column(Modifier.padding(top = 40.dp)) {
            ReplyBar(status = PreviewFixtures.status)
        }
    }
}
