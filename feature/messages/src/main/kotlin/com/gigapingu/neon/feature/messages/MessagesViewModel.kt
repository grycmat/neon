package com.gigapingu.neon.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.data.ConversationRepository
import com.gigapingu.neon.core.model.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val conversations: ConversationRepository,
) : ViewModel() {

    val state: StateFlow<AsyncState<List<Conversation>>> = conversations.state

    init {
        viewModelScope.launch { conversations.load() }
    }

    fun refresh() {
        viewModelScope.launch { conversations.refresh() }
    }

    fun loadMore() {
        viewModelScope.launch { conversations.loadMore() }
    }

    fun markRead(id: String) {
        viewModelScope.launch { runCatching { conversations.markRead(id) } }
    }

    fun delete(id: String) {
        viewModelScope.launch { runCatching { conversations.delete(id) } }
    }
}
