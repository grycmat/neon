package com.gigapingu.neon.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AccountRepository
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.model.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Pending follow requests on a locked account — authorize or reject each one. */
@HiltViewModel
class FollowRequestsViewModel @Inject constructor(
    private val accounts: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AsyncState<List<Account>>>(AsyncState.idle())
    val state: StateFlow<AsyncState<List<Account>>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!_state.value.hasData) _state.value = AsyncState.loading()
            try {
                val data = accounts.getFollowRequests()
                _state.value = AsyncState.ready(data, hasMore = false)
            } catch (e: Exception) {
                if (!_state.value.hasData) {
                    _state.value = AsyncState.error(e.message ?: "Could not load follow requests")
                }
            }
        }
    }

    fun authorize(account: Account) {
        viewModelScope.launch {
            runCatching { accounts.authorizeFollowRequest(account.id) }.onSuccess { removeLocal(account.id) }
        }
    }

    fun reject(account: Account) {
        viewModelScope.launch {
            runCatching { accounts.rejectFollowRequest(account.id) }.onSuccess { removeLocal(account.id) }
        }
    }

    private fun removeLocal(id: String) {
        _state.value.data?.let { data ->
            _state.value = _state.value.withData(data.filterNot { it.id == id })
        }
    }
}
