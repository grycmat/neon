package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.data.di.ApplicationScope
import com.gigapingu.neon.core.model.MastoNotification
import com.gigapingu.neon.core.model.NotificationRequest
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.network.ApiClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json



/** Notifications list with Room cache + status sync. */
@Singleton
class NotificationRepository @Inject constructor(
    private val api: ApiClient,
    private val cache: CacheStore,
    private val json: Json,
    @ApplicationScope private val scope: CoroutineScope,
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
            val items = fetchPage(maxId = null).distinctBy { it.id }
            _state.value = AsyncState.ready(items, hasMore = items.size >= PAGE_SIZE)
            persist(items)
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

    /** A notification arrived over the streaming connection — prepend if not already present. */
    fun prependNotification(notification: MastoNotification) {
        val current = _state.value
        val data = current.data ?: return
        if (data.any { it.id == notification.id }) return
        val updated = listOf(notification) + data
        _state.value = current.withData(updated)
        // Persist so the home-screen widget picks it up too: while the app is foregrounded
        // streaming is the delivery path (push is only for the backgrounded case), and the
        // widget renders from the cache.
        scope.launch { persist(updated) }
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

    suspend fun dismiss(id: String) {
        api.post("/api/v1/notifications/$id/dismiss")
        val current = _state.value
        val data = current.data ?: return
        val filtered = data.filterNot { it.id == id }
        _state.value = current.withData(filtered)
        persist(filtered)
    }

    suspend fun clear() {
        api.post("/api/v1/notifications/clear")
        _state.value = AsyncState.ready(emptyList(), hasMore = false)
        persist(emptyList())
    }

    /** Writes the canonical list to Room, then tells the home-screen widget to redraw from it. */
    private suspend fun persist(items: List<MastoNotification>, notifyWidget: Boolean = true) {
        cache.putList(CACHE_KEY, items, MastoNotification.serializer()) { it.id }
        if (notifyWidget) NotificationWidgetBridge.redraw()
    }

    /**
     * Newest cached notifications for the home-screen widget.
     *
     * Reads Room rather than the in-memory [state], which is usually empty anyway in a process the
     * system woke for a broadcast — and even when the app is running, Room is the fresher of the
     * two here: every writer that can change the *head* of the list ([refresh],
     * [prependNotification], [dismiss], [clear], [refreshForWidget]) persists, while
     * [refreshForWidget] deliberately leaves [state] alone. ([loadMore] only appends older pages,
     * which never reach the widget's top slice.)
     */
    suspend fun cachedNotifications(limit: Int): List<MastoNotification> =
        cache.getList(CACHE_KEY, MastoNotification.serializer()).take(limit)

    /**
     * Background refresh for the home-screen widget: fetches the newest page and writes it to the
     * Room cache, deliberately leaving [state] alone.
     *
     * It cannot just call [refresh], which moves the in-memory phase out of `Idle` — [load] returns
     * early on any non-Idle phase, so a widget update landing before the app's first load would
     * leave the Notifications screen showing whatever this background fetch produced (including its
     * error state) with no way to retry.
     */
    suspend fun refreshForWidget(): List<MastoNotification> {
        val items = fetchPage(maxId = null).distinctBy { it.id }
        // No redraw from here — the widget stamps its own "updated" time after this returns and
        // redraws once, instead of twice with a stale label in between.
        persist(items, notifyWidget = false)
        return items
    }

    suspend fun getNotification(id: String): MastoNotification {
        return json.decodeFromString(
            MastoNotification.serializer(),
            api.get("/api/v1/notifications/$id")
        )
    }

    /** Notifications held back from accounts you don't follow (Mastodon 4.3+). */
    suspend fun getRequests(maxId: String? = null, limit: Int = 40): List<NotificationRequest> {
        val query = buildMap {
            put("limit", limit)
            maxId?.let { put("max_id", it) }
        }
        return json.decodeFromString(
            ListSerializer(NotificationRequest.serializer()),
            api.get("/api/v1/notifications/requests", query),
        )
    }

    suspend fun acceptRequest(id: String) {
        api.post("/api/v1/notifications/requests/$id/accept")
    }

    suspend fun dismissRequest(id: String) {
        api.post("/api/v1/notifications/requests/$id/dismiss")
    }
}



