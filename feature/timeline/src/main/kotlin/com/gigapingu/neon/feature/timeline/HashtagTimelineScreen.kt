package com.gigapingu.neon.feature.timeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.ui.AsyncList
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.status.StatusCard
import com.gigapingu.neon.core.ui.status.StatusListSkeleton

@Composable
fun HashtagTimelineScreen(
    hashtag: String,
    viewModel: HashtagTimelineViewModel = hiltViewModel(key = "hashtag-$hashtag"),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(hashtag) {
        viewModel.start(hashtag)
    }

    NeonBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            HashtagTopBar(hashtag = hashtag)
            AsyncList(
                state = state,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadMore,
                emptyLabel = "No toots matching #$hashtag yet!",
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
                key = { it.id },
                loadingContent = { StatusListSkeleton() },
            ) { status ->
                StatusCard(status = status)
            }
        }
    }
}

@Composable
private fun HashtagTopBar(hashtag: String) {
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
        Text("#$hashtag", style = NeonTheme.type.headlineMedium, color = NeonTheme.palette.text)
    }
}
