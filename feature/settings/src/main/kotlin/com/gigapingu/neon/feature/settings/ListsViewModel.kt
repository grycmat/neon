package com.gigapingu.neon.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.data.ListRepository
import com.gigapingu.neon.core.model.MastodonList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Settings > Lists — create/rename/delete the user's own lists. */
@HiltViewModel
class ListsViewModel @Inject constructor(
    private val lists: ListRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AsyncState<List<MastodonList>>>(AsyncState.idle())
    val state: StateFlow<AsyncState<List<MastodonList>>> = _state.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!_state.value.hasData) _state.value = AsyncState.loading()
            try {
                val data = lists.getLists()
                _state.value = AsyncState.ready(data, hasMore = false)
            } catch (e: Exception) {
                if (!_state.value.hasData) {
                    _state.value = AsyncState.error(e.message ?: "Could not load lists")
                }
            }
        }
    }

    fun create(title: String) {
        val cleaned = title.trim()
        if (cleaned.isEmpty()) return
        viewModelScope.launch {
            try {
                val created = lists.createList(cleaned)
                _state.value.data?.let { _state.value = _state.value.withData(it + created) }
            } catch (e: Exception) {
                _errors.tryEmit(e.message ?: "Could not create list")
            }
        }
    }

    fun rename(list: MastodonList, title: String) {
        val cleaned = title.trim()
        if (cleaned.isEmpty()) return
        viewModelScope.launch {
            try {
                val updated = lists.updateList(list.id, cleaned, list.repliesPolicy)
                _state.value.data?.let { data ->
                    _state.value = _state.value.withData(data.map { if (it.id == updated.id) updated else it })
                }
            } catch (e: Exception) {
                _errors.tryEmit(e.message ?: "Could not rename list")
            }
        }
    }

    fun delete(list: MastodonList) {
        viewModelScope.launch {
            try {
                lists.deleteList(list.id)
                _state.value.data?.let { data ->
                    _state.value = _state.value.withData(data.filterNot { it.id == list.id })
                }
            } catch (e: Exception) {
                _errors.tryEmit(e.message ?: "Could not delete list")
            }
        }
    }
}
