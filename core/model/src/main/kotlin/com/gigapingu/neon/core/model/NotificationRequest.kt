package com.gigapingu.neon.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * NotificationRequest entity (https://docs.joinmastodon.org/entities/NotificationRequest/).
 * `notifications_count` is a numeric string on the wire.
 */
@Serializable
data class NotificationRequest(
    val id: String,
    val account: Account,
    @SerialName("notifications_count") val notificationsCountRaw: String = "0",
) {
    val notificationsCount: Int get() = notificationsCountRaw.toIntOrNull() ?: 0
}
