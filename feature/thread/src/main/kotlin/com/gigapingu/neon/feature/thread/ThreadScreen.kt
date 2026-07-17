package com.gigapingu.neon.feature.thread

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.NeonAvatar
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.component.NeonLabel
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.fullTime
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.ui.LocalNeonNavigator
import com.gigapingu.neon.core.ui.PreviewFixtures
import com.gigapingu.neon.core.ui.PreviewHarness
import com.gigapingu.neon.core.ui.status.MediaGrid
import com.gigapingu.neon.core.ui.status.PollView
import com.gigapingu.neon.core.ui.status.QuoteCard
import com.gigapingu.neon.core.ui.status.StatusActions
import com.gigapingu.neon.core.ui.status.StatusBody
import com.gigapingu.neon.core.ui.status.StatusCard

/** Thread view: ancestors → focused toot (large) → descendants. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    statusId: String,
    viewModel: ThreadViewModel = hiltViewModel(key = "thread-$statusId"),
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val navigator = LocalNeonNavigator.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(statusId) { viewModel.start(statusId) }

    NeonBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                    onClick = navigator::back,
                    contentDescription = "Back",
                )
                Spacer(Modifier.width(10.dp))
                Text("Thread", style = type.headlineMedium, color = palette.text)
            }
            val status = uiState.status
            if (status == null && uiState.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.cyan)
                }
                return@Column
            }
            PullToRefreshBox(
                isRefreshing = false,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 40.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val ancestors = uiState.context?.ancestors.orEmpty()
                    val descendants = uiState.context?.descendants.orEmpty()
                    items(count = ancestors.size, key = { "anc-" + ancestors[it].id }) { index ->
                        StatusCard(status = ancestors[index])
                    }
                    if (status != null) {
                        item(key = "focused") { FocusedStatus(status = status.display) }
                    }
                    uiState.error?.let { error ->
                        item(key = "error") {
                            Text(
                                "Could not load thread: $error",
                                style = type.bodySmall,
                                color = palette.textDim,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    if (descendants.isNotEmpty()) {
                        item(key = "replies-label") {
                            NeonLabel(
                                "Replies",
                                modifier = Modifier.padding(start = 6.dp, top = 18.dp, end = 6.dp, bottom = 8.dp),
                            )
                        }
                        items(count = descendants.size, key = { "dec-" + descendants[it].id }) { index ->
                            StatusCard(status = descendants[index])
                        }
                    }
                }
            }
        }
    }
}

/** The focused toot — larger type, full timestamp, prominent actions. */
@Composable
private fun FocusedStatus(status: Status) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val navigator = LocalNeonNavigator.current
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
                ) { navigator.openProfile(status.account.id) },
            ) {
                NeonAvatar(account = status.account, size = 46.dp, ring = true)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(status.account.displayNameOrUsername, style = type.titleMedium, color = palette.text)
                    Text(status.account.fullHandle, style = type.bodySmall, color = palette.textDim)
                }
            }
            Spacer(Modifier.height(14.dp))
            StatusBody(status = status, textStyle = type.bodyLarge.copy(fontSize = 16.sp))
            status.quote?.let { quoted ->
                QuoteCard(status = quoted, onClick = { navigator.openThread(quoted.id) })
            }
            if (status.mediaAttachments.isNotEmpty()) {
                MediaGrid(attachments = status.mediaAttachments)
            }
            status.poll?.let { PollView(poll = it) }
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
