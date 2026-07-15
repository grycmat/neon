package com.gigapingu.neon.feature.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.StatusRepository
import com.gigapingu.neon.core.data.patchPollList
import com.gigapingu.neon.core.data.patchStatusList
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.model.StatusContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ThreadUiState(
    val status: Status? = null,
    val context: StatusContext? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val statuses: StatusRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThreadUiState())
    val uiState: StateFlow<ThreadUiState> = _uiState.asStateFlow()

    private var statusId: String? = null

    init {
        // Keep the thread in sync with interactions made anywhere.
        viewModelScope.launch {
            statuses.updates.collect { updated ->
                _uiState.update { state ->
                    state.copy(
                        status = state.status?.let { patchStatusList(listOf(it), updated).first() },
                        context = state.context?.let {
                            StatusContext(
                                ancestors = patchStatusList(it.ancestors, updated),
                                descendants = patchStatusList(it.descendants, updated),
                            )
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            statuses.pollUpdates.collect { poll ->
                _uiState.update { state ->
                    state.copy(
                        status = state.status?.let { patchPollList(listOf(it), poll).first() },
                        context = state.context?.let {
                            StatusContext(
                                ancestors = patchPollList(it.ancestors, poll),
                                descendants = patchPollList(it.descendants, poll),
                            )
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            statuses.created.collect { created ->
                // A reply may have been posted from here — refresh descendants.
                if (created.inReplyToId != null) statusId?.let { load(it) }
            }
        }
    }

    fun start(statusId: String) {
        if (this.statusId == statusId) return
        this.statusId = statusId
        load(statusId)
    }

    fun refresh() {
        statusId?.let { load(it) }
    }

    private fun load(id: String) {
        _uiState.update { it.copy(error = null) }
        viewModelScope.launch {
            try {
                coroutineScope {
                    val status = async { statuses.getStatus(id) }
                    val context = async { statuses.getContext(id) }
                    _uiState.update {
                        it.copy(status = status.await(), context = context.await(), loading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message ?: "Could not load thread") }
            }
        }
    }
}
