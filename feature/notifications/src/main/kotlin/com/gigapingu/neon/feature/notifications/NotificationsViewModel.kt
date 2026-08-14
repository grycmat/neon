package com.gigapingu.neon.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AccountRepository
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.data.NotificationKind
import com.gigapingu.neon.core.data.NotificationRepository
import com.gigapingu.neon.core.model.MastoNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notifications: NotificationRepository,
    private val accounts: AccountRepository,
) : ViewModel() {

    private val _kind = MutableStateFlow(NotificationKind.All)
    val kind: StateFlow<NotificationKind> = _kind.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    val state: StateFlow<AsyncState<List<MastoNotification>>> =
        combine(notifications.state, _kind) { state, kind ->
            if (kind == NotificationKind.All) state
            else state.copy(data = state.data?.filter { kind.matches(it.type) })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AsyncState.idle())

    fun switchTo(kind: NotificationKind) {
        _kind.value = kind
    }

    private val _requestsCount = MutableStateFlow(0)
    val requestsCount: StateFlow<Int> = _requestsCount.asStateFlow()

    private val _followRequestsCount = MutableStateFlow(0)
    val followRequestsCount: StateFlow<Int> = _followRequestsCount.asStateFlow()

    init {
        viewModelScope.launch { notifications.load() }
        refreshRequestsCount()
        refreshFollowRequestsCount()
    }

    fun refreshRequestsCount() {
        viewModelScope.launch {
            runCatching { notifications.getRequests() }
                .onSuccess { _requestsCount.value = it.sumOf { r -> r.notificationsCount } }
        }
    }

    fun refreshFollowRequestsCount() {
        viewModelScope.launch {
            runCatching { accounts.getFollowRequests() }
                .onSuccess { _followRequestsCount.value = it.size }
        }
    }

    fun authorizeFollow(notification: MastoNotification) {
        viewModelScope.launch {
            runCatching { accounts.authorizeFollowRequest(notification.account.id) }
                .onSuccess {
                    runCatching { notifications.dismiss(notification.id) }
                    refreshFollowRequestsCount()
                }
        }
    }

    fun rejectFollow(notification: MastoNotification) {
        viewModelScope.launch {
            runCatching { accounts.rejectFollowRequest(notification.account.id) }
                .onSuccess {
                    runCatching { notifications.dismiss(notification.id) }
                    refreshFollowRequestsCount()
                }
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
                .onFailure { _errors.tryEmit("Couldn't dismiss notification: ${it.message}") }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            runCatching { notifications.clear() }
        }
    }
}
