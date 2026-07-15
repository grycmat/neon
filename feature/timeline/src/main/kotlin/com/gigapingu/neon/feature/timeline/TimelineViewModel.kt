package com.gigapingu.neon.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.data.TimelineKind
import com.gigapingu.neon.core.data.TimelineRepository
import com.gigapingu.neon.core.model.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val timelines: TimelineRepository,
) : ViewModel() {

    private val _kind = MutableStateFlow(TimelineKind.Home)
    val kind: StateFlow<TimelineKind> = _kind.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<AsyncState<List<Status>>> =
        _kind.flatMapLatest { timelines.timeline(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AsyncState.idle())

    init {
        load()
    }

    fun switchTo(kind: TimelineKind) {
        _kind.value = kind
        load()
    }

    fun load() {
        viewModelScope.launch { timelines.load(_kind.value) }
    }

    fun refresh() {
        viewModelScope.launch { timelines.refresh(_kind.value) }
    }

    fun loadMore() {
        viewModelScope.launch { timelines.loadMore(_kind.value) }
    }
}
