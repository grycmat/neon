package com.gigapingu.neon.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.data.FilterRepository
import com.gigapingu.neon.core.model.ServerFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Settings > Filters — keyword filters (v2 API). */
@HiltViewModel
class FiltersViewModel @Inject constructor(
    private val filters: FilterRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AsyncState<List<ServerFilter>>>(AsyncState.idle())
    val state: StateFlow<AsyncState<List<ServerFilter>>> = _state.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!_state.value.hasData) _state.value = AsyncState.loading()
            try {
                val data = filters.getFilters()
                _state.value = AsyncState.ready(data, hasMore = false)
            } catch (e: Exception) {
                if (!_state.value.hasData) {
                    _state.value = AsyncState.error(e.message ?: "Could not load filters")
                }
            }
        }
    }

    fun save(
        editing: ServerFilter?,
        title: String,
        phrase: String,
        contexts: List<String>,
        wholeWord: Boolean,
        filterAction: String,
        expiresInSeconds: Int?,
    ) {
        val cleanedTitle = title.trim().ifEmpty { phrase.trim() }
        val cleanedPhrase = phrase.trim()
        if (cleanedPhrase.isEmpty() || contexts.isEmpty()) return
        viewModelScope.launch {
            try {
                val saved = if (editing != null) {
                    filters.updateFilter(editing, cleanedTitle, cleanedPhrase, contexts, wholeWord, filterAction, expiresInSeconds)
                } else {
                    filters.createFilter(cleanedTitle, cleanedPhrase, contexts, wholeWord, filterAction, expiresInSeconds)
                }
                _state.value.data?.let { data ->
                    val next = if (editing != null) {
                        data.map { if (it.id == saved.id) saved else it }
                    } else {
                        data + saved
                    }
                    _state.value = _state.value.withData(next)
                }
            } catch (e: Exception) {
                _errors.tryEmit(e.message ?: "Could not save filter")
            }
        }
    }

    fun delete(filter: ServerFilter) {
        viewModelScope.launch {
            try {
                filters.deleteFilter(filter.id)
                _state.value.data?.let { data ->
                    _state.value = _state.value.withData(data.filterNot { it.id == filter.id })
                }
            } catch (e: Exception) {
                _errors.tryEmit(e.message ?: "Could not delete filter")
            }
        }
    }
}
