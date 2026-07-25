package com.gigapingu.neon.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.data.TagRepository
import com.gigapingu.neon.core.model.TrendTag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Hashtags the user follows — Settings > Followed hashtags. */
@HiltViewModel
class FollowedHashtagsViewModel @Inject constructor(
    private val tags: TagRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AsyncState<List<TrendTag>>>(AsyncState.idle())
    val state: StateFlow<AsyncState<List<TrendTag>>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!_state.value.hasData) _state.value = AsyncState.loading()
            try {
                val list = tags.getFollowedTags(limit = 100)
                _state.value = AsyncState.ready(list, hasMore = false)
            } catch (e: Exception) {
                if (!_state.value.hasData) {
                    _state.value = AsyncState.error(e.message ?: "Could not load followed hashtags")
                }
            }
        }
    }

    fun unfollow(tag: TrendTag) {
        viewModelScope.launch {
            runCatching { tags.unfollowTag(tag.name) }
                .onSuccess {
                    _state.value.data?.let { data ->
                        _state.value = _state.value.withData(data.filterNot { it.name == tag.name })
                    }
                }
        }
    }
}
