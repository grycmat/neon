package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.model.Conversation
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.network.ApiClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Direct-message conversations list with Room cache + status sync. */
@Singleton
class ConversationRepository @Inject constructor(
    private val api: ApiClient,
    private val cache: CacheStore,
    private val json: Json,
) {
    private companion object {
        const val CACHE_KEY = "conversations"
        const val PAGE_SIZE = 20
    }

    private val _state = MutableStateFlow<AsyncState<List<Conversation>>>(AsyncState.idle())
    val state: StateFlow<AsyncState<List<Conversation>>> = _state.asStateFlow()

    /** Called by StatusRepository after a favourite/boost/edit so last_status stays in sync. */
    fun applyStatusUpdate(updated: Status) {
        val current = _state.value
        val data = current.data ?: return
        _state.value = current.withData(
            data.map { conversation ->
                val status = conversation.lastStatus ?: return@map conversation
                conversation.copy(lastStatus = patchStatusList(listOf(status), updated).first())
            },
        )
    }

    fun applyStatusDelete(deletedId: String) {
        val current = _state.value
        val data = current.data ?: return
        _state.value = current.withData(
            data.filterNot { it.lastStatus?.id == deletedId },
        )
    }

    /**
     * Called by StatusRepository after posting a new direct-visibility status.
     * The create endpoint only returns the bare Status (no Conversation), so
     * there's no cheap local patch — just refetch.
     */
    suspend fun onDirectStatusCreated() {
        refresh()
    }

    suspend fun load() {
        if (_state.value.phase != AsyncPhase.Idle) return
        _state.value = AsyncState.loading()
        val cached = cache.getList(CACHE_KEY, Conversation.serializer())
        if (cached.isNotEmpty()) {
            _state.value = AsyncState.ready(cached)
        }
        refresh()
    }

    suspend fun refresh() {
        if (_state.value.hasData) {
            _state.value = _state.value.withPhase(AsyncPhase.Refreshing)
        }
        try {
            val items = fetchPage(maxId = null)
            _state.value = AsyncState.ready(items.distinctBy { it.id }, hasMore = items.size >= PAGE_SIZE)
            cache.putList(CACHE_KEY, items, Conversation.serializer()) { it.id }
        } catch (e: Exception) {
            _state.value = if (_state.value.hasData) {
                _state.value.withPhase(AsyncPhase.Ready)
            } else {
                AsyncState.error(e.message ?: "Could not load messages")
            }
        }
    }

    suspend fun loadMore() {
        val state = _state.value
        val data = state.data
        if (data == null || !state.hasMore || state.phase == AsyncPhase.LoadingMore) return
        _state.value = state.withPhase(AsyncPhase.LoadingMore)
        try {
            val more = fetchPage(maxId = data.last().id)
            val current = _state.value.data ?: data
            val seen = current.mapTo(HashSet()) { it.id }
            _state.value = state.withData(
                current + more.filterNot { it.id in seen }.distinctBy { it.id },
                hasMore = more.size >= PAGE_SIZE,
            )
        } catch (_: Exception) {
            _state.value = state.withPhase(AsyncPhase.Ready)
        }
    }

    private suspend fun fetchPage(maxId: String?): List<Conversation> {
        val query = buildMap {
            put("limit", PAGE_SIZE)
            maxId?.let { put("max_id", it) }
        }
        return json.decodeFromString(
            ListSerializer(Conversation.serializer()),
            api.get("/api/v1/conversations", query),
        )
    }

    suspend fun markRead(id: String) {
        api.post("/api/v1/conversations/$id/read")
        val current = _state.value
        val data = current.data ?: return
        val updated = data.map { if (it.id == id) it.copy(unread = false) else it }
        _state.value = current.withData(updated)
        cache.putList(CACHE_KEY, updated, Conversation.serializer()) { it.id }
    }

    suspend fun delete(id: String) {
        api.delete("/api/v1/conversations/$id")
        val current = _state.value
        val data = current.data ?: return
        val filtered = data.filterNot { it.id == id }
        _state.value = current.withData(filtered)
        cache.putList(CACHE_KEY, filtered, Conversation.serializer()) { it.id }
    }
}
