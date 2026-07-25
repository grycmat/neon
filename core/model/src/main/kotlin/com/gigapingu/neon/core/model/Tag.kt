package com.gigapingu.neon.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * FeaturedTag entity (https://docs.joinmastodon.org/entities/FeaturedTag/).
 * `statuses_count` is a numeric string on the wire.
 */
@Serializable
data class FeaturedTag(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    @SerialName("statuses_count") val statusesCountRaw: String = "0",
) {
    val statusesCount: Int get() = statusesCountRaw.toIntOrNull() ?: 0
}
