package com.gigapingu.neon.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.data.BookmarkRepository
import com.gigapingu.neon.core.model.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarks: BookmarkRepository,
) : ViewModel() {

    val state: StateFlow<AsyncState<List<Status>>> = bookmarks.bookmarks

    init {
        viewModelScope.launch { bookmarks.load() }
    }

    fun refresh() {
        viewModelScope.launch { bookmarks.refresh() }
    }

    fun loadMore() {
        viewModelScope.launch { bookmarks.loadMore() }
    }
}
