package com.gigapingu.neon.feature.settings

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassButton
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.compactCount
import com.gigapingu.neon.core.model.TrendTag
import com.gigapingu.neon.core.ui.AsyncList
import com.gigapingu.neon.core.ui.Navigator

/** Settings > Followed hashtags — the hashtags this account follows. */
@Composable
fun ManageFollowedHashtagsScreen(viewModel: FollowedHashtagsViewModel = hiltViewModel()) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
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
            Text("Followed hashtags", style = type.headlineMedium, color = palette.text)
        }
        AsyncList(
            state = state,
            onRefresh = viewModel::refresh,
            emptyLabel = "You're not following any hashtags yet",
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
            key = { it.name },
        ) { tag ->
            FollowedHashtagRow(tag = tag, onUnfollow = { viewModel.unfollow(tag) })
        }
    }
}

@Composable
private fun FollowedHashtagRow(tag: TrendTag, onUnfollow: () -> Unit) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        contentPadding = PaddingValues(14.dp),
        onClick = { Navigator.openHashtag(tag.name) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("#${tag.name}", style = type.titleSmall, color = palette.text)
                Text("${compactCount(tag.uses)} recent uses", style = type.bodySmall, color = palette.textDim)
            }
            Spacer(Modifier.width(10.dp))
            GlassButton(label = "Unfollow", height = 36.dp, onClick = onUnfollow)
        }
    }
}
