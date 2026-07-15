package com.gigapingu.neon.core.model

import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** MediaAttachment entity (https://docs.joinmastodon.org/entities/MediaAttachment/). */
enum class MediaType(val wire: String) {
    Image("image"), Gifv("gifv"), Video("video"), Audio("audio"), Unknown("unknown");

    companion object {
        fun parse(raw: String): MediaType = entries.firstOrNull { it.wire == raw } ?: Unknown
    }
}

@Serializable
data class MediaAttachment(
    val id: String,
    @SerialName("type") val rawType: String = "unknown",
    val url: String = "",
    @SerialName("preview_url") val previewUrl: String? = null,
    val description: String? = null,
    val blurhash: String? = null,
) {
    val type: MediaType get() = MediaType.parse(rawType)
    val preview: String get() = previewUrl?.takeIf { it.isNotEmpty() } ?: url
    val altText: String get() = description.orEmpty()
    val isPlayable: Boolean get() = type == MediaType.Video || type == MediaType.Gifv
}

/** Poll entity (https://docs.joinmastodon.org/entities/Poll/). */
@Serializable
data class Poll(
    val id: String,
    @SerialName("expires_at")
    @Serializable(with = LenientInstantSerializer::class)
    val expiresAt: Instant? = null,
    val expired: Boolean = false,
    val multiple: Boolean = false,
    @SerialName("votes_count") val votesCount: Int = 0,
    @SerialName("voters_count") val votersCount: Int? = null,
    val voted: Boolean = false,
    @SerialName("own_votes") val ownVotes: List<Int> = emptyList(),
    val options: List<PollOption> = emptyList(),
) {
    val showResults: Boolean get() = voted || expired
}

@Serializable
data class PollOption(
    val title: String = "",
    @SerialName("votes_count") val votesCount: Int = 0,
)

/** Draft poll used by the composer (POST /api/v1/statuses `poll` param). */
data class PollDraft(
    val options: List<String>,
    val expiresInSeconds: Int,
    val multiple: Boolean,
)
