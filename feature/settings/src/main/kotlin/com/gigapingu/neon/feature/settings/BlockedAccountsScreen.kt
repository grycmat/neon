package com.gigapingu.neon.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassButton
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.ui.AccountRow
import com.gigapingu.neon.core.ui.AsyncList
import com.gigapingu.neon.core.ui.Navigator

/** Settings > Blocked & muted accounts — lets a user review and undo either list. */
@Composable
fun BlockedAccountsScreen(viewModel: BlockedAccountsViewModel = hiltViewModel()) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val blocked by viewModel.blocked.collectAsStateWithLifecycle()
    val muted by viewModel.muted.collectAsStateWithLifecycle()

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
            Text("Blocked & muted", style = type.headlineMedium, color = palette.text)
        }
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TabChip(
                label = "Blocked",
                selected = tab == BlockedAccountsTab.Blocked,
                onClick = { viewModel.selectTab(BlockedAccountsTab.Blocked) },
                modifier = Modifier.weight(1f),
            )
            TabChip(
                label = "Muted",
                selected = tab == BlockedAccountsTab.Muted,
                onClick = { viewModel.selectTab(BlockedAccountsTab.Muted) },
                modifier = Modifier.weight(1f),
            )
        }
        if (tab == BlockedAccountsTab.Blocked) {
            AsyncList(
                state = blocked,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadMore,
                emptyLabel = "You haven't blocked anyone",
                modifier = Modifier.fillMaxSize(),
                key = { it.id },
            ) { account ->
                AccountRow(
                    account = account,
                    trailing = { GlassButton(label = "Unblock", height = 36.dp, onClick = { viewModel.unblock(account) }) },
                )
            }
        } else {
            AsyncList(
                state = muted,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadMore,
                emptyLabel = "You haven't muted anyone",
                modifier = Modifier.fillMaxSize(),
                key = { it.id },
            ) { account ->
                AccountRow(
                    account = account,
                    trailing = { GlassButton(label = "Unmute", height = 36.dp, onClick = { viewModel.unmute(account) }) },
                )
            }
        }
    }
}

@Composable
private fun TabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) palette.cyan.copy(alpha = .1f) else palette.surface, shape)
            .border(1.dp, if (selected) palette.cyan.copy(alpha = .4f) else palette.border, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = type.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = if (selected) palette.cyan else palette.textDim,
        )
    }
}
