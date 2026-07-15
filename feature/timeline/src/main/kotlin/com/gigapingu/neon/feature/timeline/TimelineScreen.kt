package com.gigapingu.neon.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.data.TimelineKind
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.ui.AsyncList
import com.gigapingu.neon.core.ui.status.StatusCard

/** Home / Local / Federated timelines behind a segmented pill switcher. */
@Composable
fun TimelineScreen(viewModel: TimelineViewModel = hiltViewModel()) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val kind by viewModel.kind.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    NeonBackground {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
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
            AsyncList(
                state = state,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadMore,
                emptyLabel = "No toots yet — follow some people!",
                key = { it.id },
            ) { status ->
                StatusCard(status = status)
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
