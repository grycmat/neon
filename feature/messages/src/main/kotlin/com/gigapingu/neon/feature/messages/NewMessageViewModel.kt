package com.gigapingu.neon.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.SearchRepository
import com.gigapingu.neon.core.model.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@HiltViewModel
class NewMessageViewModel @Inject constructor(
    private val search: SearchRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val _results = MutableStateFlow<List<Account>>(emptyList())
    val results: StateFlow<List<Account>> = _results.asStateFlow()

    init {
        viewModelScope.launch {
            query.debounce(300).collectLatest { q ->
                _results.value = if (q.length < 2) emptyList() else {
                    runCatching { search.searchAccounts(q, limit = 20) }
                        .getOrDefault(emptyList())
                        .distinctBy { it.id }
                }
            }
        }
    }

    fun onQueryChange(q: String) {
        query.value = q
    }
}
