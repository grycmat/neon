package com.gigapingu.neon.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.ListRepository
import com.gigapingu.neon.core.model.MastodonList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListMembershipUiState(
    val allLists: List<MastodonList> = emptyList(),
    val memberIds: Set<String> = emptySet(),
    val loading: Boolean = true,
    val error: String? = null,
)

/** Backs the "add to list" sheet opened from another account's profile. */
@HiltViewModel
class ListMembershipViewModel @Inject constructor(
    private val lists: ListRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ListMembershipUiState())
    val state: StateFlow<ListMembershipUiState> = _state.asStateFlow()

    private var accountId: String? = null

    fun start(accountId: String) {
        if (this.accountId == accountId) return
        this.accountId = accountId
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val all = lists.getLists().distinctBy { it.id }
                val member = lists.getListsContaining(accountId).mapTo(HashSet()) { it.id }
                _state.update { it.copy(allLists = all, memberIds = member, loading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Could not load lists") }
            }
        }
    }

    fun toggle(list: MastodonList) {
        val id = accountId ?: return
        val isMember = list.id in _state.value.memberIds
        _state.update { s ->
            s.copy(memberIds = if (isMember) s.memberIds - list.id else s.memberIds + list.id)
        }
        viewModelScope.launch {
            try {
                if (isMember) lists.removeAccount(list.id, id) else lists.addAccount(list.id, id)
            } catch (e: Exception) {
                _state.update { s ->
                    s.copy(
                        memberIds = if (isMember) s.memberIds + list.id else s.memberIds - list.id,
                        error = e.message ?: "Could not update list",
                    )
                }
            }
        }
    }
}
