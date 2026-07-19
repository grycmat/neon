package com.gigapingu.neon.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AsyncPhase
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.data.CacheStore
import com.gigapingu.neon.core.data.StatusRepository
import com.gigapingu.neon.core.data.patchPollList
import com.gigapingu.neon.core.data.patchStatusList
import com.gigapingu.neon.core.model.Poll
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.network.ApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@HiltViewModel
class HashtagTimelineViewModel @Inject constructor(
    private val api: ApiClient,
    private val cache: CacheStore,
    private val json: Json,
    private val statusRepo: StatusRepository,
) : ViewModel(), StatusRepository.StatusListener {

    private companion object {
        const val PAGE_SIZE = 20
    }

    private var hashtag: String? = null
    private val cacheKey: String get() = "hashtag:${hashtag.orEmpty()}"
    private val path: String get() = "/api/v1/timelines/tag/${hashtag.orEmpty()}"

    private val _state = MutableStateFlow<AsyncState<List<Status>>>(AsyncState.idle())
    val state: StateFlow<AsyncState<List<Status>>> = _state.asStateFlow()

    init {
        statusRepo.addListener(this)
    }

    override fun onCleared() {
        statusRepo.removeListener(this)
        super.onCleared()
    }

    fun start(hashtag: String) {
        if (this.hashtag == hashtag) return
        this.hashtag = hashtag
        load()
    }

    private fun load() {
        if (_state.value.phase != AsyncPhase.Idle) return
        _state.value = AsyncState.loading()
        viewModelScope.launch {
            val cached = cache.getList(cacheKey, Status.serializer())
            if (cached.isNotEmpty()) {
                _state.value = AsyncState.ready(cached)
            }
            refresh()
        }
    }

    fun refresh() {
        if (_state.value.hasData) {
            _state.value = _state.value.withPhase(AsyncPhase.Refreshing)
        }
        viewModelScope.launch {
            try {
                val page = fetchPage(maxId = null)
                _state.value = AsyncState.ready(page, hasMore = page.size >= PAGE_SIZE)
                cache.putList(cacheKey, page, Status.serializer()) { it.id }
            } catch (e: Exception) {
                _state.value = if (_state.value.hasData) {
                    _state.value.withPhase(AsyncPhase.Ready)
                } else {
                    AsyncState.error(e.message ?: "Could not load timeline")
                }
            }
        }
    }

    fun loadMore() {
        val state = _state.value
        val data = state.data
        if (data == null || !state.hasMore || state.phase == AsyncPhase.LoadingMore) return
        _state.value = state.withPhase(AsyncPhase.LoadingMore)
        viewModelScope.launch {
            try {
                val more = fetchPage(maxId = data.last().id)
                val current = _state.value.data ?: data
                val seen = current.mapTo(HashSet()) { it.id }
                _state.value = _state.value.withData(
                    current + more.filterNot { it.id in seen },
                    hasMore = more.size >= PAGE_SIZE,
                )
            } catch (_: Exception) {
                _state.value = _state.value.withPhase(AsyncPhase.Ready)
            }
        }
    }

    private suspend fun fetchPage(maxId: String?): List<Status> {
        val query = buildMap {
            put("limit", PAGE_SIZE)
            maxId?.let { put("max_id", it) }
        }
        val response = api.get(path, query)
        return json.decodeFromString(ListSerializer(Status.serializer()), response)
    }

    // StatusListener
    override fun onStatusUpdated(status: Status) {
        _state.value.data?.let { data ->
            _state.value = _state.value.withData(patchStatusList(data, status))
        }
    }

    override fun onPollUpdated(poll: Poll) {
        _state.value.data?.let { data ->
            _state.value = _state.value.withData(patchPollList(data, poll))
        }
    }

    override fun onStatusDeleted(id: String) {
        _state.value.data?.let { data ->
            _state.value = _state.value.withData(
                data.filterNot { it.id == id || it.reblog?.id == id }
            )
        }
    }
}
