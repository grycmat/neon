package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.data.di.ApplicationScope
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.network.ApiClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TimelineKind(val label: String) {
    Home("Home"), Local("Local"), Federated("Federated");

    val cacheKey: String get() = "timeline:${name.lowercase()}"
    val path: String
        get() = when (this) {
            Home -> "/api/v1/timelines/home"
            else -> "/api/v1/timelines/public"
        }
    val baseQuery: Map<String, Any?>
        get() = when (this) {
            Local -> mapOf("local" to "true")
            else -> emptyMap()
        }
}

/**
 * Holds the three timelines. Cache-first: the cached page renders instantly,
 * then the network refresh replaces it. Subscribes to StatusRepository to
 * keep interaction state in sync across screens.
 */
@Singleton
class TimelineRepository @Inject constructor(
    private val api: ApiClient,
    private val cache: CacheStore,
    private val statuses: StatusRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    private companion object {
        const val PAGE_SIZE = 20
    }

    private val timelines: Map<TimelineKind, MutableStateFlow<AsyncState<List<Status>>>> =
        TimelineKind.entries.associateWith { MutableStateFlow(AsyncState.idle()) }

    init {
        scope.launch { statuses.created.collect(::onCreated) }
        scope.launch { statuses.updates.collect(::onStatusUpdate) }
        scope.launch {
            statuses.pollUpdates.collect { poll ->
                patchAll { list -> patchPollList(list, poll) }
            }
        }
    }

    fun timeline(kind: TimelineKind): StateFlow<AsyncState<List<Status>>> =
        timelines.getValue(kind).asStateFlow()

    /** Initial load: cache first, then network. */
    suspend fun load(kind: TimelineKind) {
        val flow = timelines.getValue(kind)
        if (flow.value.phase != AsyncPhase.Idle) return
        flow.value = AsyncState.loading()
        val cached = cache.getList(kind.cacheKey, Status.serializer())
        if (cached.isNotEmpty()) {
            flow.value = AsyncState.ready(cached)
        }
        refresh(kind)
    }

    suspend fun refresh(kind: TimelineKind) {
        val flow = timelines.getValue(kind)
        if (flow.value.hasData) {
            flow.value = flow.value.withPhase(AsyncPhase.Refreshing)
        }
        try {
            val page = fetchPage(kind, maxId = null)
            flow.value = AsyncState.ready(page, hasMore = page.size >= PAGE_SIZE)
            cache.putList(kind.cacheKey, page, Status.serializer()) { it.id }
        } catch (e: Exception) {
            flow.value = if (flow.value.hasData) {
                flow.value.withPhase(AsyncPhase.Ready)
            } else {
                AsyncState.error(e.message ?: "Could not load timeline")
            }
        }
    }

    suspend fun loadMore(kind: TimelineKind) {
        val flow = timelines.getValue(kind)
        val state = flow.value
        val data = state.data
        if (data == null || !state.hasMore || state.phase == AsyncPhase.LoadingMore) return
        flow.value = state.withPhase(AsyncPhase.LoadingMore)
        try {
            val more = fetchPage(kind, maxId = data.last().id)
            flow.value = state.withData(data + more, hasMore = more.size >= PAGE_SIZE)
        } catch (_: Exception) {
            flow.value = state.withPhase(AsyncPhase.Ready)
        }
    }

    private suspend fun fetchPage(kind: TimelineKind, maxId: String?): List<Status> {
        val query = buildMap {
            putAll(kind.baseQuery)
            put("limit", PAGE_SIZE)
            maxId?.let { put("max_id", it) }
        }
        return statuses.decodeStatusList(api.get(kind.path, query))
    }

    private fun onCreated(status: Status) {
        val home = timelines.getValue(TimelineKind.Home)
        home.value.data?.let { data ->
            home.value = home.value.withData(listOf(status) + data)
        }
    }

    private fun onStatusUpdate(updated: Status) {
        patchAll { list -> patchStatusList(list, updated) }
    }

    private fun patchAll(patch: (List<Status>) -> List<Status>) {
        timelines.values.forEach { flow ->
            flow.value.data?.let { data -> flow.value = flow.value.withData(patch(data)) }
        }
    }
}
