package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.model.MastoNotification
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.network.ApiClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Notifications list with Room cache + status sync. */
@Singleton
class NotificationRepository @Inject constructor(
    private val api: ApiClient,
    private val cache: CacheStore,
    private val json: Json,
) {
    private companion object {
        const val CACHE_KEY = "notifications"
        const val PAGE_SIZE = 25
    }

    private val _state = MutableStateFlow<AsyncState<List<MastoNotification>>>(AsyncState.idle())
    val state: StateFlow<AsyncState<List<MastoNotification>>> = _state.asStateFlow()

    /** Called by StatusRepository after a favourite/boost so wrapped statuses stay in sync. */
    fun applyStatusUpdate(updated: Status) {
        val current = _state.value
        val data = current.data ?: return
        _state.value = current.withData(
            data.map { notification ->
                val status = notification.status ?: return@map notification
                notification.copy(status = patchStatusList(listOf(status), updated).first())
            },
        )
    }

    suspend fun load() {
        if (_state.value.phase != AsyncPhase.Idle) return
        _state.value = AsyncState.loading()
        val cached = cache.getList(CACHE_KEY, MastoNotification.serializer())
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
            _state.value = AsyncState.ready(items, hasMore = items.size >= PAGE_SIZE)
            cache.putList(CACHE_KEY, items, MastoNotification.serializer()) { it.id }
        } catch (e: Exception) {
            _state.value = if (_state.value.hasData) {
                _state.value.withPhase(AsyncPhase.Ready)
            } else {
                AsyncState.error(e.message ?: "Could not load notifications")
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
            _state.value = state.withData(data + more, hasMore = more.size >= PAGE_SIZE)
        } catch (_: Exception) {
            _state.value = state.withPhase(AsyncPhase.Ready)
        }
    }

    fun applyStatusDelete(deletedId: String) {
        val current = _state.value
        val data = current.data ?: return
        _state.value = current.withData(
            data.filterNot { it.status?.id == deletedId || it.status?.reblog?.id == deletedId }
        )
    }

    private suspend fun fetchPage(maxId: String?): List<MastoNotification> {
        val query = buildMap {
            put("limit", PAGE_SIZE)
            maxId?.let { put("max_id", it) }
        }
        return json.decodeFromString(
            ListSerializer(MastoNotification.serializer()),
            api.get("/api/v1/notifications", query),
        )
    }
}
