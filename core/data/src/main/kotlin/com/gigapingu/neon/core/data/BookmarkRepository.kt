package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.model.Poll
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.network.ApiClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Singleton
class BookmarkRepository @Inject constructor(
    private val api: ApiClient,
    private val cache: CacheStore,
    private val json: Json,
) {
    private companion object {
        const val PAGE_SIZE = 20
        const val CACHE_KEY = "bookmarks"
    }

    private val _bookmarksState = MutableStateFlow<AsyncState<List<Status>>>(AsyncState.idle())
    val bookmarks: StateFlow<AsyncState<List<Status>>> = _bookmarksState.asStateFlow()

    suspend fun load() {
        if (_bookmarksState.value.phase != AsyncPhase.Idle) return
        _bookmarksState.value = AsyncState.loading()
        val cached = cache.getList(CACHE_KEY, Status.serializer())
        if (cached.isNotEmpty()) {
            _bookmarksState.value = AsyncState.ready(cached)
        }
        refresh()
    }

    suspend fun refresh() {
        if (_bookmarksState.value.hasData) {
            _bookmarksState.value = _bookmarksState.value.withPhase(AsyncPhase.Refreshing)
        }
        try {
            val page = fetchPage(maxId = null)
            _bookmarksState.value = AsyncState.ready(page, hasMore = page.size >= PAGE_SIZE)
            cache.putList(CACHE_KEY, page, Status.serializer()) { it.id }
        } catch (e: Exception) {
            _bookmarksState.value = if (_bookmarksState.value.hasData) {
                _bookmarksState.value.withPhase(AsyncPhase.Ready)
            } else {
                AsyncState.error(e.message ?: "Could not load bookmarks")
            }
        }
    }

    suspend fun loadMore() {
        val state = _bookmarksState.value
        val data = state.data
        if (data == null || !state.hasMore || state.phase == AsyncPhase.LoadingMore) return
        _bookmarksState.value = state.withPhase(AsyncPhase.LoadingMore)
        try {
            val more = fetchPage(maxId = data.last().id)
            val current = _bookmarksState.value.data ?: data
            val seen = current.mapTo(HashSet()) { it.id }
            _bookmarksState.value = _bookmarksState.value.withData(
                current + more.filterNot { it.id in seen },
                hasMore = more.size >= PAGE_SIZE,
            )
        } catch (_: Exception) {
            _bookmarksState.value = _bookmarksState.value.withPhase(AsyncPhase.Ready)
        }
    }

    private suspend fun fetchPage(maxId: String?): List<Status> {
        val query = buildMap {
            put("limit", PAGE_SIZE)
            maxId?.let { put("max_id", it) }
        }
        return json.decodeFromString(ListSerializer(Status.serializer()), api.get("/api/v1/bookmarks", query))
    }

    fun applyStatusUpdate(updated: Status) {
        _bookmarksState.value.data?.let { data ->
            val patched = patchStatusList(data, updated)
            val filtered = patched.filter { it.bookmarked }
            _bookmarksState.value = _bookmarksState.value.withData(filtered)
        }
    }

    fun applyPollUpdate(poll: Poll) {
        _bookmarksState.value.data?.let { data ->
            _bookmarksState.value = _bookmarksState.value.withData(patchPollList(data, poll))
        }
    }

    fun applyStatusDelete(deletedId: String) {
        _bookmarksState.value.data?.let { data ->
            _bookmarksState.value = _bookmarksState.value.withData(
                data.filterNot { it.id == deletedId || it.reblog?.id == deletedId }
            )
        }
    }
}
