package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.model.Instance
import com.gigapingu.neon.core.network.ApiClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The current instance's configuration (currently: status character limit).
 * Fetched once per session and cached in memory — an instance's own limits
 * don't change without an admin upgrade, so there's no need to re-fetch on
 * every composer open. [clear] drops the cache on logout so a session
 * against a different instance doesn't reuse a stale limit.
 */
@Singleton
class InstanceRepository @Inject constructor(
    private val api: ApiClient,
    private val json: Json,
) {
    @Volatile private var cached: Instance? = null
    private val mutex = Mutex()

    suspend fun getInstance(): Instance {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: fetch().also { cached = it }
        }
    }

    fun clear() {
        cached = null
    }

    private suspend fun fetch(): Instance =
        runCatching { json.decodeFromString(Instance.serializer(), api.get("/api/v2/instance")) }
            .getOrElse { fetchLegacy() }

    /** Pre-v2 servers (Mastodon < 3.4, some forks): flat `max_toot_chars` / `urls.streaming_api` fields. */
    private suspend fun fetchLegacy(): Instance = runCatching {
        val body = json.parseToJsonElement(api.get("/api/v1/instance")).jsonObject
        val maxChars = body["max_toot_chars"]?.jsonPrimitive?.intOrNull
            ?: Instance.DEFAULT_MAX_CHARACTERS
        val streamingUrl = body["urls"]?.jsonObject?.get("streaming_api")?.jsonPrimitive?.contentOrNull
        Instance(
            configuration = Instance.Configuration(
                statuses = Instance.Statuses(maxCharacters = maxChars),
                urls = Instance.Urls(streaming = streamingUrl),
            ),
        )
    }.getOrDefault(Instance())
}
