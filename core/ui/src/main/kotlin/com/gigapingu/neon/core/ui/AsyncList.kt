package com.gigapingu.neon.core.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.data.AsyncPhase
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.designsystem.component.GlassButton
import com.gigapingu.neon.core.designsystem.theme.NeonMotion
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import kotlinx.coroutines.flow.distinctUntilChanged

private enum class ListPane { Loading, Error, Content }

/**
 * Pull-to-refresh + infinite-scroll list bound to an [AsyncState] of items.
 * Single responsibility: list plumbing; item rendering is delegated.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> AsyncList(
    state: AsyncState<List<T>>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onLoadMore: (() -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    emptyLabel: String = "Nothing here yet",
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 90.dp),
    key: ((T) -> Any)? = null,
    header: (@Composable () -> Unit)? = null,
    loadingContent: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    val palette = NeonTheme.palette
    val items = state.data.orEmpty()

    // Infinite scroll: trigger when the last few items become visible.
    if (onLoadMore != null) {
        val shouldLoadMore by remember(listState, items.size) {
            derivedStateOf {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                items.isNotEmpty() && lastVisible >= listState.layoutInfo.totalItemsCount - 4
            }
        }
        LaunchedEffect(listState) {
            snapshotFlow { shouldLoadMore }
                .distinctUntilChanged()
                .collect { if (it) onLoadMore() }
        }
    }

    val pane = when {
        state.isLoading && !state.hasData -> ListPane.Loading
        state.isError -> ListPane.Error
        else -> ListPane.Content
    }

    Crossfade(
        targetState = pane,
        animationSpec = NeonMotion.quick(),
        label = "asyncListPane",
        modifier = modifier,
    ) { current ->
        when (current) {
            ListPane.Loading -> loadingContent?.invoke() ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = palette.cyan)
            }

            ListPane.Error -> ErrorPane(
                message = state.error ?: "Something broke",
                onRetry = onRefresh,
            )

            ListPane.Content -> PullToRefreshBox(
                isRefreshing = state.phase == AsyncPhase.Refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = contentPadding,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (header != null) {
                        item(key = "header") { header() }
                    }
                    if (items.isEmpty()) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 60.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(emptyLabel, style = NeonTheme.type.bodyMedium, color = palette.textMute)
                            }
                        }
                    } else {
                        items(count = items.size, key = key?.let { k -> { index: Int -> k(items[index]) } }) { index ->
                            // New/patched rows glide into place instead of popping.
                            Box(Modifier.animateItem()) {
                                itemContent(items[index])
                            }
                        }
                    }
                    item(key = "footer") {
                        if (state.phase == AsyncPhase.LoadingMore) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.2.dp,
                                    color = palette.cyan,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Friendly error pane with retry. */
@Composable
fun ErrorPane(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val palette = NeonTheme.palette
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = palette.textMute,
                modifier = Modifier.size(34.dp),
            )
            Text(
                message,
                textAlign = TextAlign.Center,
                style = NeonTheme.type.bodyMedium,
                color = palette.textDim,
            )
            if (onRetry != null) {
                GlassButton(label = "Retry", height = 44.dp, onClick = onRetry)
            }
        }
    }
}
