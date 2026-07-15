package com.gigapingu.neon.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.SearchRepository
import com.gigapingu.neon.core.model.SearchResults
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.model.TrendTag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExploreUiState(
    val query: String = "",
    val tags: List<TrendTag> = emptyList(),
    val trending: List<Status> = emptyList(),
    val results: SearchResults? = null,
    val loadingTrends: Boolean = true,
    val searching: Boolean = false,
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val search: SearchRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private var initialized = false

    /** Idempotent init; [initialQuery] is set when opened from a hashtag tap. */
    fun start(initialQuery: String?) {
        if (initialized) return
        initialized = true
        loadTrends()
        if (!initialQuery.isNullOrEmpty()) {
            _uiState.update { it.copy(query = initialQuery) }
            search()
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun clearSearch() {
        _uiState.update { it.copy(query = "", results = null) }
    }

    fun searchTag(tag: String) {
        _uiState.update { it.copy(query = "#$tag") }
        search()
    }

    fun loadTrends() {
        viewModelScope.launch {
            try {
                coroutineScope {
                    val tags = async { search.trendingTags() }
                    val trending = async { search.trendingStatuses() }
                    _uiState.update {
                        it.copy(tags = tags.await(), trending = trending.await(), loadingTrends = false)
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(loadingTrends = false) }
            }
        }
    }

    fun search() {
        val q = _uiState.value.query.trim()
        if (q.isEmpty()) {
            _uiState.update { it.copy(results = null) }
            return
        }
        _uiState.update { it.copy(searching = true) }
        viewModelScope.launch {
            try {
                val results = search.search(q)
                _uiState.update { it.copy(results = results, searching = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(searching = false) }
                _errors.tryEmit("Search failed: ${e.message}")
            }
        }
    }
}
