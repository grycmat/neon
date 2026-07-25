package com.gigapingu.neon.core.model

import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** FilterKeyword entity (https://docs.joinmastodon.org/entities/FilterKeyword/). */
@Serializable
data class FilterKeyword(
    val id: String = "",
    val keyword: String = "",
    @SerialName("whole_word") val wholeWord: Boolean = true,
)

/** Filter entity, v2 API (https://docs.joinmastodon.org/entities/Filter/). */
@Serializable
data class ServerFilter(
    val id: String,
    val title: String = "",
    val context: List<String> = emptyList(),
    @SerialName("expires_at")
    @Serializable(with = LenientInstantSerializer::class)
    val expiresAt: Instant? = null,
    @SerialName("filter_action") val filterAction: String = "warn",
    val keywords: List<FilterKeyword> = emptyList(),
) {
    /** Neon authors one keyword per filter (matching most client UIs). */
    val phrase: String get() = keywords.firstOrNull()?.keyword.orEmpty()
    val wholeWord: Boolean get() = keywords.firstOrNull()?.wholeWord ?: true
}

/** One entry of Status.filtered — a filter that matched this status. */
@Serializable
data class FilterResult(
    val filter: ServerFilter,
    @SerialName("keyword_matches") val keywordMatches: List<String>? = null,
    @SerialName("status_matches") val statusMatches: List<String>? = null,
)

object FilterContext {
    const val Home = "home"
    const val Notifications = "notifications"
    const val Public = "public"
    const val Thread = "thread"
    const val Account = "account"
    val All = listOf(Home, Notifications, Public, Thread, Account)
}
