package com.gigapingu.neon.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.data.NotificationRepository
import com.gigapingu.neon.core.model.NotificationRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Notifications held back from accounts you don't follow — accept or dismiss them. */
@HiltViewModel
class NotificationRequestsViewModel @Inject constructor(
    private val notifications: NotificationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AsyncState<List<NotificationRequest>>>(AsyncState.idle())
    val state: StateFlow<AsyncState<List<NotificationRequest>>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!_state.value.hasData) _state.value = AsyncState.loading()
            try {
                val data = notifications.getRequests()
                _state.value = AsyncState.ready(data, hasMore = false)
            } catch (e: Exception) {
                if (!_state.value.hasData) {
                    _state.value = AsyncState.error(e.message ?: "Could not load requests")
                }
            }
        }
    }

    fun accept(request: NotificationRequest) {
        viewModelScope.launch {
            runCatching { notifications.acceptRequest(request.id) }.onSuccess { removeLocal(request.id) }
        }
    }

    fun dismiss(request: NotificationRequest) {
        viewModelScope.launch {
            runCatching { notifications.dismissRequest(request.id) }.onSuccess { removeLocal(request.id) }
        }
    }

    private fun removeLocal(id: String) {
        _state.value.data?.let { data ->
            _state.value = _state.value.withData(data.filterNot { it.id == id })
        }
    }
}
