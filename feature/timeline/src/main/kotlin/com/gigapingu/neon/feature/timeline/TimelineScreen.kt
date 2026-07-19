package com.gigapingu.neon.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.data.TimelineKind
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.ui.AsyncList
import com.gigapingu.neon.core.ui.LocalShellPadding
import com.gigapingu.neon.core.ui.PaneSelection
import com.gigapingu.neon.core.ui.PreviewHarness
import com.gigapingu.neon.core.ui.status.StatusCard
import com.gigapingu.neon.core.ui.status.StatusListSkeleton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import com.gigapingu.neon.core.designsystem.component.GlassButton
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Home / Local / Federated timelines behind a segmented pill switcher.
 * [selectedStatusId] marks the toot open in the big-screen detail pane.
 */
@Composable
fun TimelineScreen(
    selectedStatusId: String? = null,
    viewModel: TimelineViewModel = hiltViewModel(),
) {
    val palette = NeonTheme.palette
    val kind by viewModel.kind.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val newTootsCount by viewModel.currentNewTootsCount.collectAsStateWithLifecycle()
    val shellPadding = LocalShellPadding.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Pills float over the list (like HomeShell's bars) so items actually
    // scroll up behind the translucent top app bar instead of stopping below it.
    var pillsHeightPx by remember { mutableIntStateOf(0) }
    val pillsHeight = with(LocalDensity.current) { pillsHeightPx.toDp() }

    LaunchedEffect(listState, kind) {
        snapshotFlow { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
            .distinctUntilChanged()
            .collect { isAtTop ->
                if (isAtTop) {
                    viewModel.clearNewToots(kind)
                }
            }
    }

    NeonBackground {
        Box(Modifier.fillMaxSize()) {
            AsyncList(
                state = state,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadMore,
                emptyLabel = "No toots yet — follow some people!",
                modifier = Modifier.fillMaxSize(),
                listState = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = pillsHeight,
                    end = 16.dp,
                    bottom = 90.dp + shellPadding.calculateBottomPadding(),
                ),
                key = { it.id },
                loadingContent = { StatusListSkeleton() },
            ) { status ->
                PaneSelection(selected = status.display.id == selectedStatusId) {
                    StatusCard(status = status)
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(palette.surfaceSolid.copy(alpha = .80f))
                    .onSizeChanged { pillsHeightPx = it.height }
                    .padding(top = shellPadding.calculateTopPadding())
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimelineKind.entries.forEach { entry ->
                    SegmentPill(
                        label = entry.label,
                        active = entry == kind,
                        onClick = { viewModel.switchTo(entry) },
                    )
                }
            }

            AnimatedVisibility(
                visible = newTootsCount > 0,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = pillsHeight + 8.dp)
            ) {
                GlassButton(
                    label = "↑ $newTootsCount new toots",
                    tinted = true,
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                        viewModel.clearNewToots(kind)
                    }
                )
            }
        }
    }
}

@Composable
private fun SegmentPill(label: String, active: Boolean, onClick: () -> Unit) {
    val palette = NeonTheme.palette
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(shape)
            .background(if (active) palette.surfaceHi else Color.Transparent)
            .border(1.dp, if (active) palette.borderStrong else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = NeonTheme.type.labelLarge.copy(fontSize = 14.sp),
            color = if (active) palette.text else palette.textDim,
        )
    }
}

@Preview(name = "Timeline segment pills", showBackground = true, heightDp = 100)
@Composable
private fun SegmentPillPreview() {
    PreviewHarness {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TimelineKind.entries.forEachIndexed { index, entry ->
                SegmentPill(label = entry.label, active = index == 0, onClick = {})
            }
        }
    }
}
