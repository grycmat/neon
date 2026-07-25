package com.gigapingu.neon.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Custom emoji entity (https://docs.joinmastodon.org/entities/CustomEmoji/). */
@Serializable
data class Emoji(
    val shortcode: String = "",
    val url: String = "",
    @SerialName("static_url") val staticUrl: String = "",
)
