package com.gigapingu.neon.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AccountRepository
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.data.StatusRepository
import com.gigapingu.neon.core.data.patchPollList
import com.gigapingu.neon.core.data.patchStatusList
import com.gigapingu.neon.core.model.Account
import com.gigapingu.neon.core.model.Poll
import com.gigapingu.neon.core.model.Relationship
import com.gigapingu.neon.core.model.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val account: Account? = null,
    val relationship: Relationship? = null,
    val statuses: List<Status> = emptyList(),
    val loadingStatuses: Boolean = true,
    val hasMore: Boolean = true,
    val followBusy: Boolean = false,
    val isSelf: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val accounts: AccountRepository,
    private val auth: AuthRepository,
    private val statusRepository: StatusRepository,
) : ViewModel(), StatusRepository.StatusListener {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private var accountId: String? = null

    init {
        // Keep this profile's toots in sync with interactions made anywhere.
        statusRepository.addListener(this)
        // Keep the self profile in sync after edits.
        viewModelScope.launch {
            auth.me.collect { me ->
                if (me != null && me.id == accountId) {
                    _uiState.update { it.copy(account = me) }
                }
            }
        }
    }

    override fun onCleared() {
        statusRepository.removeListener(this)
    }

    override fun onStatusUpdated(status: Status) {
        _uiState.update { it.copy(statuses = patchStatusList(it.statuses, status)) }
    }

    override fun onPollUpdated(poll: Poll) {
        _uiState.update { it.copy(statuses = patchPollList(it.statuses, poll)) }
    }

    fun start(accountId: String) {
        if (this.accountId == accountId) return
        this.accountId = accountId
        _uiState.update { it.copy(isSelf = auth.me.value?.id == accountId) }
        load()
    }

    fun load() {
        val id = accountId ?: return
        viewModelScope.launch {
            // Cache-first profile.
            accounts.getCachedAccount(id)?.let { cached ->
                _uiState.update { it.copy(account = cached) }
            }
            runCatching { accounts.getAccount(id) }
                .onSuccess { fresh -> _uiState.update { it.copy(account = fresh) } }
            if (!_uiState.value.isSelf) {
                runCatching { accounts.getRelationship(id) }
                    .onSuccess { rel -> _uiState.update { it.copy(relationship = rel) } }
            }
            runCatching { accounts.getStatuses(id) }
                .onSuccess { statuses ->
                    _uiState.update {
                        it.copy(statuses = statuses, loadingStatuses = false, hasMore = statuses.size >= 20)
                    }
                }
                .onFailure { _uiState.update { it.copy(loadingStatuses = false) } }
        }
    }

    fun loadMore() {
        val id = accountId ?: return
        val state = _uiState.value
        if (!state.hasMore || state.statuses.isEmpty()) return
        viewModelScope.launch {
            runCatching { accounts.getStatuses(id, maxId = state.statuses.last().id) }
                .onSuccess { more ->
                    _uiState.update {
                        it.copy(statuses = it.statuses + more, hasMore = more.size >= 20)
                    }
                }
        }
    }

    fun toggleFollow() {
        val id = accountId ?: return
        val rel = _uiState.value.relationship ?: return
        if (_uiState.value.followBusy) return
        _uiState.update { it.copy(followBusy = true) }
        viewModelScope.launch {
            try {
                val updated = accounts.setFollowing(id, !(rel.following || rel.requested))
                _uiState.update { it.copy(relationship = updated) }
            } catch (e: Exception) {
                _errors.tryEmit(e.message ?: "Could not update follow state")
            } finally {
                _uiState.update { it.copy(followBusy = false) }
            }
        }
    }
}
