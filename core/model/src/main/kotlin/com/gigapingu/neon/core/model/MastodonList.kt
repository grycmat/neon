package com.gigapingu.neon.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** List entity (https://docs.joinmastodon.org/entities/List/). */
@Serializable
data class MastodonList(
    val id: String,
    val title: String = "",
    @SerialName("replies_policy") val repliesPolicy: String = "list",
)
