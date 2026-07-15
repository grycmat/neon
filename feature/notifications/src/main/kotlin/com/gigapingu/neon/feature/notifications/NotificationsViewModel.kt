package com.gigapingu.neon.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.data.NotificationRepository
import com.gigapingu.neon.core.model.MastoNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notifications: NotificationRepository,
) : ViewModel() {

    val state: StateFlow<AsyncState<List<MastoNotification>>> = notifications.state

    init {
        viewModelScope.launch { notifications.load() }
    }

    fun refresh() {
        viewModelScope.launch { notifications.refresh() }
    }

    fun loadMore() {
        viewModelScope.launch { notifications.loadMore() }
    }
}
