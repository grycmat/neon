package com.gigapingu.neon.feature.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.ui.AccountRow
import com.gigapingu.neon.core.ui.AsyncList
import com.gigapingu.neon.core.ui.Navigator

/** Notifications > Follow requests — authorize or reject pending requests to follow a locked account. */
@Composable
fun FollowRequestsScreen(viewModel: FollowRequestsViewModel = hiltViewModel()) {
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
            Text("Follow requests", style = type.headlineMedium, color = palette.text)
        }
        AsyncList(
            state = state,
            onRefresh = viewModel::refresh,
            emptyLabel = "No pending follow requests",
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
            key = { it.id },
        ) { account ->
            AccountRow(
                account = account,
                trailing = {
                    Row {
                        GlassIconButton(
                            icon = Icons.Rounded.Close,
                            onClick = { viewModel.reject(account) },
                            contentDescription = "Reject follow request from @${account.acct}",
                        )
                        Spacer(Modifier.width(8.dp))
                        GlassIconButton(
                            icon = Icons.Rounded.Check,
                            tinted = true,
                            onClick = { viewModel.authorize(account) },
                            contentDescription = "Authorize follow request from @${account.acct}",
                        )
                    }
                },
            )
        }
    }
}
