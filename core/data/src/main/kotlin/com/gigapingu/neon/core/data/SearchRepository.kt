package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.model.Account
import com.gigapingu.neon.core.model.SearchResults
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.model.TrendTag
import com.gigapingu.neon.core.network.ApiClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Search + trends (Explore tab, and @-mention autocomplete in the composer). */
@Singleton
class SearchRepository @Inject constructor(
    private val api: ApiClient,
    private val json: Json,
) {
    suspend fun search(q: String, type: String? = null, limit: Int = 20): SearchResults {
        if (q.isBlank()) return SearchResults.Empty
        val query = buildMap {
            put("q", q)
            put("resolve", true)
            put("limit", limit)
            type?.let { put("type", it) }
        }
        return json.decodeFromString(SearchResults.serializer(), api.get("/api/v2/search", query))
    }

    /** Lightweight account lookup for composer @-autocomplete. */
    suspend fun searchAccounts(q: String, limit: Int = 6): List<Account> {
        if (q.isBlank()) return emptyList()
        return json.decodeFromString(
            ListSerializer(Account.serializer()),
            api.get("/api/v1/accounts/search", mapOf("q" to q, "limit" to limit)),
        )
    }

    suspend fun trendingTags(): List<TrendTag> =
        json.decodeFromString(
            ListSerializer(TrendTag.serializer()),
            api.get("/api/v1/trends/tags", mapOf("limit" to 10)),
        )

    suspend fun trendingStatuses(): List<Status> =
        json.decodeFromString(
            ListSerializer(Status.serializer()),
            api.get("/api/v1/trends/statuses", mapOf("limit" to 20)),
        )
}
