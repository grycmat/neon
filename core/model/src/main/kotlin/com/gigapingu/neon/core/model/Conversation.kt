package com.gigapingu.neon.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Direct-message thread (https://docs.joinmastodon.org/entities/Conversation/).
 * Mastodon has no separate DM system — this just groups statuses posted with
 * visibility="direct" by participant set.
 */
@Serializable
data class Conversation(
    val id: String,
    val unread: Boolean = false,
    val accounts: List<Account> = emptyList(),
    @SerialName("last_status") val lastStatus: Status? = null,
)
