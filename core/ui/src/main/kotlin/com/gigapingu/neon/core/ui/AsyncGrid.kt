package com.gigapingu.neon.core.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.data.AsyncPhase
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.designsystem.theme.NeonMotion
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import kotlinx.coroutines.flow.distinctUntilChanged

private enum class GridPane { Loading, Error, Content }

/**
 * Grid counterpart to [AsyncList]: same pull-to-refresh/infinite-scroll/loading-error
 * plumbing, laid out as a fixed-column staggered grid instead of a single column —
 * used by the big-screen "grid" layout, where card heights vary too much for a plain
 * [androidx.compose.foundation.lazy.grid.LazyVerticalGrid] to look right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> AsyncGrid(
    state: AsyncState<List<T>>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    onLoadMore: (() -> Unit)? = null,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    emptyLabel: String = "Nothing here yet",
    contentPadding: PaddingValues = PaddingValues(12.dp),
    key: ((T) -> Any)? = null,
    header: (@Composable () -> Unit)? = null,
    loadingContent: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    val palette = NeonTheme.palette
    val items = state.data.orEmpty()

    // Same infinite-scroll trigger as AsyncList, adapted to the grid's layout info.
    if (onLoadMore != null) {
        val loadMore by rememberUpdatedState(onLoadMore)
        val canLoadMore by rememberUpdatedState(items.isNotEmpty() && state.hasMore)
        LaunchedEffect(gridState) {
            snapshotFlow {
                val info = gridState.layoutInfo
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                canLoadMore && info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 4
            }
                .distinctUntilChanged()
                .collect { if (it) loadMore() }
        }
    }

    val pane = when {
        state.isLoading && !state.hasData -> GridPane.Loading
        state.isError -> GridPane.Error
        else -> GridPane.Content
    }

    Crossfade(
        targetState = pane,
        animationSpec = NeonMotion.quick(),
        label = "asyncGridPane",
        modifier = modifier,
    ) { current ->
        when (current) {
            GridPane.Loading -> loadingContent?.invoke() ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = palette.cyan)
            }

            GridPane.Error -> ErrorPane(
                message = state.error ?: "Something broke",
                onRetry = onRefresh,
            )

            GridPane.Content -> PullToRefreshBox(
                isRefreshing = state.phase == AsyncPhase.Refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(top = 60.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Text(emptyLabel, style = NeonTheme.type.bodyMedium, color = palette.textMute)
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(columns),
                        state = gridState,
                        contentPadding = contentPadding,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (header != null) {
                            item(key = "header", span = StaggeredGridItemSpan.FullLine) { header() }
                        }
                        items(
                            count = items.size,
                            key = key?.let { k -> { index: Int -> k(items[index]) } },
                        ) { index ->
                            Box(Modifier.animateItem()) {
                                itemContent(items[index])
                            }
                        }
                        if (state.phase == AsyncPhase.LoadingMore) {
                            item(
                                key = "footer",
                                span = StaggeredGridItemSpan.FullLine,
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
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
}
