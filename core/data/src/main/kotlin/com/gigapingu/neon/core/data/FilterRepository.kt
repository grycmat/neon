package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.model.ServerFilter
import com.gigapingu.neon.core.network.ApiClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** Keyword filters, v2 API (https://docs.joinmastodon.org/methods/filters/). */
@Singleton
class FilterRepository @Inject constructor(
    private val api: ApiClient,
    private val json: Json,
) {
    suspend fun getFilters(): List<ServerFilter> =
        json.decodeFromString(ListSerializer(ServerFilter.serializer()), api.get("/api/v2/filters"))

    suspend fun createFilter(
        title: String,
        phrase: String,
        contexts: List<String>,
        wholeWord: Boolean,
        filterAction: String,
        expiresInSeconds: Int?,
    ): ServerFilter = json.decodeFromString(
        ServerFilter.serializer(),
        api.post("/api/v2/filters", filterBody(title, phrase, contexts, wholeWord, filterAction, expiresInSeconds)),
    )

    suspend fun updateFilter(
        filter: ServerFilter,
        title: String,
        phrase: String,
        contexts: List<String>,
        wholeWord: Boolean,
        filterAction: String,
        expiresInSeconds: Int?,
    ): ServerFilter = json.decodeFromString(
        ServerFilter.serializer(),
        api.put(
            "/api/v2/filters/${filter.id}",
            filterBody(title, phrase, contexts, wholeWord, filterAction, expiresInSeconds, filter.keywords.firstOrNull()?.id),
        ),
    )

    suspend fun deleteFilter(id: String) {
        api.delete("/api/v2/filters/$id")
    }

    private fun filterBody(
        title: String,
        phrase: String,
        contexts: List<String>,
        wholeWord: Boolean,
        filterAction: String,
        expiresInSeconds: Int?,
        keywordId: String? = null,
    ) = buildJsonObject {
        put("title", title)
        putJsonArray("context") { contexts.forEach { add(it) } }
        put("filter_action", filterAction)
        if (expiresInSeconds != null) put("expires_in", expiresInSeconds) else put("expires_in", "")
        putJsonArray("keywords_attributes") {
            add(
                buildJsonObject {
                    keywordId?.let { put("id", it) }
                    put("keyword", phrase)
                    put("whole_word", wholeWord)
                },
            )
        }
    }.toString()
}
