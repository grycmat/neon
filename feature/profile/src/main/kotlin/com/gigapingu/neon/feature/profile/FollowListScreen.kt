package com.gigapingu.neon.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AccountRepository
import com.gigapingu.neon.core.data.AsyncPhase
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.model.Account
import com.gigapingu.neon.core.ui.AccountRow
import com.gigapingu.neon.core.ui.AsyncList
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.isBigScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val accounts: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AsyncState<List<Account>>>(AsyncState.idle())
    val state: StateFlow<AsyncState<List<Account>>> = _state.asStateFlow()

    private var accountId: String? = null
    private var following = false

    fun start(accountId: String, following: Boolean) {
        if (this.accountId == accountId) return
        this.accountId = accountId
        this.following = following
        refresh()
    }

    private suspend fun fetch(maxId: String?): List<Account> {
        val id = checkNotNull(accountId)
        return if (following) accounts.getFollowing(id, maxId) else accounts.getFollowers(id, maxId)
    }

    fun refresh() {
        viewModelScope.launch {
            if (!_state.value.hasData) _state.value = AsyncState.loading()
            try {
                val list = fetch(maxId = null)
                _state.value = AsyncState.ready(list, hasMore = list.size >= 40)
            } catch (e: Exception) {
                if (!_state.value.hasData) {
                    _state.value = AsyncState.error(e.message ?: "Could not load accounts")
                }
            }
        }
    }

    fun loadMore() {
        val state = _state.value
        val data = state.data
        if (data == null || data.isEmpty() || !state.hasMore || state.phase == AsyncPhase.LoadingMore) return
        _state.value = state.withPhase(AsyncPhase.LoadingMore)
        viewModelScope.launch {
            try {
                val more = fetch(maxId = data.last().id)
                _state.value = state.withData(data + more, hasMore = more.size >= 40)
            } catch (_: Exception) {
                _state.value = state.withPhase(AsyncPhase.Ready)
            }
        }
    }
}

/** Followers / following list with infinite scroll. */
@Composable
fun FollowListScreen(
    accountId: String,
    handle: String,
    following: Boolean,
    viewModel: FollowListViewModel = hiltViewModel(key = "followlist-$accountId-$following"),
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(accountId, following) { viewModel.start(accountId, following) }

    NeonBackground {
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
                Column {
                    Text(
                        if (following) "Following" else "Followers",
                        style = type.headlineMedium,
                        color = palette.text,
                    )
                    Text(handle, style = type.bodySmall, color = palette.textDim)
                }
            }
            if (isBigScreen()) {
                // Two-column directory (design 12): rows are chunked pairs so
                // AsyncList keeps its pull-to-refresh + infinite scroll.
                val pairedState = AsyncState(
                    phase = state.phase,
                    data = state.data?.chunked(2),
                    error = state.error,
                    hasMore = state.hasMore,
                )
                AsyncList(
                    state = pairedState,
                    onRefresh = viewModel::refresh,
                    onLoadMore = viewModel::loadMore,
                    emptyLabel = "Nobody here yet",
                    key = { it.first().id },
                ) { pair ->
                    Row {
                        Box(Modifier.weight(1f)) {
                            AccountRow(account = pair[0])
                        }
                        Spacer(Modifier.width(12.dp))
                        Box(Modifier.weight(1f)) {
                            pair.getOrNull(1)?.let { AccountRow(account = it) }
                        }
                    }
                }
            } else {
                AsyncList(
                    state = state,
                    onRefresh = viewModel::refresh,
                    onLoadMore = viewModel::loadMore,
                    emptyLabel = "Nobody here yet",
                    key = { it.id },
                ) { account ->
                    AccountRow(account = account)
                }
            }
        }
    }
}
