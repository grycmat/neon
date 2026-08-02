package com.gigapingu.neon.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AccountRepository
import com.gigapingu.neon.core.data.AsyncPhase
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.model.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BlockedAccountsTab { Blocked, Muted }

/** Settings > Blocked & muted accounts. */
@HiltViewModel
class BlockedAccountsViewModel @Inject constructor(
    private val accounts: AccountRepository,
) : ViewModel() {

    private val _tab = MutableStateFlow(BlockedAccountsTab.Blocked)
    val tab: StateFlow<BlockedAccountsTab> = _tab.asStateFlow()

    private val _blocked = MutableStateFlow<AsyncState<List<Account>>>(AsyncState.idle())
    val blocked: StateFlow<AsyncState<List<Account>>> = _blocked.asStateFlow()

    private val _muted = MutableStateFlow<AsyncState<List<Account>>>(AsyncState.idle())
    val muted: StateFlow<AsyncState<List<Account>>> = _muted.asStateFlow()

    init {
        refresh()
    }

    fun selectTab(tab: BlockedAccountsTab) {
        _tab.value = tab
        refresh()
    }

    fun refresh() {
        when (_tab.value) {
            BlockedAccountsTab.Blocked -> load(_blocked, accounts::getBlocks) { _blocked.value = it }
            BlockedAccountsTab.Muted -> load(_muted, accounts::getMutes) { _muted.value = it }
        }
    }

    private fun load(
        state: MutableStateFlow<AsyncState<List<Account>>>,
        fetch: suspend (String?) -> List<Account>,
        update: (AsyncState<List<Account>>) -> Unit,
    ) {
        viewModelScope.launch {
            if (!state.value.hasData) update(AsyncState.loading())
            try {
                val list = fetch(null)
                update(AsyncState.ready(list.distinctBy { it.id }, hasMore = list.size >= 40))
            } catch (e: Exception) {
                if (!state.value.hasData) {
                    update(AsyncState.error(e.message ?: "Could not load accounts"))
                }
            }
        }
    }

    fun loadMore() {
        val stateFlow = if (_tab.value == BlockedAccountsTab.Blocked) _blocked else _muted
        val fetch: suspend (String?) -> List<Account> =
            if (_tab.value == BlockedAccountsTab.Blocked) accounts::getBlocks else accounts::getMutes
        val state = stateFlow.value
        val data = state.data
        if (data == null || data.isEmpty() || !state.hasMore || state.phase == AsyncPhase.LoadingMore) return
        stateFlow.value = state.withPhase(AsyncPhase.LoadingMore)
        viewModelScope.launch {
            try {
                val more = fetch(data.last().id)
                val current = stateFlow.value.data ?: data
                val seen = current.mapTo(HashSet()) { it.id }
                stateFlow.value = state.withData(
                    current + more.filterNot { it.id in seen }.distinctBy { it.id },
                    hasMore = more.size >= 40,
                )
            } catch (_: Exception) {
                stateFlow.value = state.withPhase(AsyncPhase.Ready)
            }
        }
    }

    fun unblock(account: Account) {
        viewModelScope.launch {
            runCatching { accounts.unblock(account.id) }
                .onSuccess {
                    _blocked.value.data?.let { data ->
                        _blocked.value = _blocked.value.withData(data.filterNot { it.id == account.id })
                    }
                }
        }
    }

    fun unmute(account: Account) {
        viewModelScope.launch {
            runCatching { accounts.unmute(account.id) }
                .onSuccess {
                    _muted.value.data?.let { data ->
                        _muted.value = _muted.value.withData(data.filterNot { it.id == account.id })
                    }
                }
        }
    }
}
