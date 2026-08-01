package com.gigapingu.neon.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Instance entity (https://docs.joinmastodon.org/entities/Instance/), v2 shape.
 * Only the fields Neon actually needs are modeled; everything else is ignored.
 */
@Serializable
data class Instance(
    val configuration: Configuration = Configuration(),
) {
    @Serializable
    data class Configuration(
        val statuses: Statuses = Statuses(),
        val urls: Urls = Urls(),
    )

    @Serializable
    data class Statuses(
        @SerialName("max_characters") val maxCharacters: Int = DEFAULT_MAX_CHARACTERS,
    )

    /** The Websockets URL for connecting to the streaming API, e.g. "wss://streaming.example.social". */
    @Serializable
    data class Urls(
        val streaming: String? = null,
    )

    val maxStatusCharacters: Int get() = configuration.statuses.maxCharacters
    val streamingUrl: String? get() = configuration.urls.streaming

    companion object {
        /** Vanilla Mastodon's own default, used until the real instance config is fetched. */
        const val DEFAULT_MAX_CHARACTERS = 500
    }
}
