package com.gigapingu.neon.core.model

import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Notification entity (https://docs.joinmastodon.org/entities/Notification/). */
enum class NotificationType(val wire: String) {
    Mention("mention"),
    Status("status"),
    Reblog("reblog"),
    Follow("follow"),
    FollowRequest("follow_request"),
    Favourite("favourite"),
    Poll("poll"),
    Update("update"),
    Quote("quote"),
    Unknown("");

    companion object {
        fun parse(raw: String): NotificationType =
            entries.firstOrNull { it.wire == raw } ?: Unknown
    }
}

@Serializable
data class MastoNotification(
    val id: String,
    @SerialName("type") val rawType: String = "",
    @SerialName("created_at")
    @Serializable(with = LenientInstantSerializer::class)
    val createdAt: Instant? = null,
    val account: Account,
    val status: Status? = null,
) {
    val type: NotificationType get() = NotificationType.parse(rawType)
}
