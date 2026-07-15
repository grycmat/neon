package com.gigapingu.neon.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/** GET /api/v2/search results. */
@Serializable
data class SearchResults(
    val accounts: List<Account> = emptyList(),
    val statuses: List<Status> = emptyList(),
    @SerialName("hashtags") val hashtags: List<TrendTag> = emptyList(),
) {
    companion object {
        val Empty = SearchResults()
    }
}

/** Tag entity with usage history (trends). Uses/accounts are summed over the history window. */
@Serializable
data class TrendTag(
    val name: String = "",
    val url: String = "",
    val history: List<JsonElement> = emptyList(),
) {
    val uses: Int get() = history.sumOf { it.intField("uses") }
    val accounts: Int get() = history.sumOf { it.intField("accounts") }

    private fun JsonElement.intField(key: String): Int =
        ((this as? JsonObject)?.get(key))?.let {
            runCatching { it.jsonPrimitive.content.toInt() }.getOrNull()
        } ?: 0
}
