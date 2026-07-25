package com.gigapingu.neon.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.data.NotificationRepository
import com.gigapingu.neon.core.model.MastoNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notifications: NotificationRepository,
) : ViewModel() {

    val state: StateFlow<AsyncState<List<MastoNotification>>> = notifications.state

    private val _requestsCount = MutableStateFlow(0)
    val requestsCount: StateFlow<Int> = _requestsCount.asStateFlow()

    init {
        viewModelScope.launch { notifications.load() }
        refreshRequestsCount()
    }

    fun refreshRequestsCount() {
        viewModelScope.launch {
            runCatching { notifications.getRequests() }
                .onSuccess { _requestsCount.value = it.sumOf { r -> r.notificationsCount } }
        }
    }

    fun refresh() {
        viewModelScope.launch { notifications.refresh() }
    }

    fun loadMore() {
        viewModelScope.launch { notifications.loadMore() }
    }

    fun dismiss(id: String) {
        viewModelScope.launch {
            runCatching { notifications.dismiss(id) }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            runCatching { notifications.clear() }
        }
    }
}
