package com.gigapingu.neon.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.ui.ErrorPane

/** Bottom sheet toggling which of the logged-in user's lists an account belongs to. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListMembershipSheet(
    accountId: String,
    onDismiss: () -> Unit,
    viewModel: ListMembershipViewModel = hiltViewModel(),
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(accountId) { viewModel.start(accountId) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceSolid,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Text(
                "Add to list",
                style = type.titleMedium,
                color = palette.text,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            when {
                state.error != null && state.allLists.isEmpty() -> ErrorPane(
                    message = state.error!!,
                    modifier = Modifier.height(180.dp),
                )
                state.loading -> Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = palette.cyan)
                }
                state.allLists.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("You haven't made any lists yet", style = type.bodyMedium, color = palette.textMute)
                }
                else -> LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(state.allLists, key = { it.id }) { list ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(list.title, style = type.bodyMedium, color = palette.text, modifier = Modifier.weight(1f))
                            Switch(
                                checked = list.id in state.memberIds,
                                onCheckedChange = { viewModel.toggle(list) },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = palette.cyan.copy(alpha = .35f),
                                    checkedThumbColor = palette.cyan,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
