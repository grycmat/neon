package com.gigapingu.neon.core.model

import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mastodon Account entity (https://docs.joinmastodon.org/entities/Account/). */
@Serializable
data class Account(
    val id: String,
    val username: String = "",
    val acct: String = "",
    @SerialName("display_name") val displayName: String = "",
    /** HTML. */
    val note: String = "",
    val url: String = "",
    val avatar: String = "",
    val header: String = "",
    @SerialName("followers_count") val followersCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    @SerialName("statuses_count") val statusesCount: Int = 0,
    val locked: Boolean = false,
    val bot: Boolean = false,
    @SerialName("created_at")
    @Serializable(with = LenientInstantSerializer::class)
    val createdAt: Instant? = null,
    val fields: List<AccountField> = emptyList(),
) {
    val displayNameOrUsername: String get() = displayName.ifEmpty { username }
    val fullHandle: String get() = "@$acct"
}

@Serializable
data class AccountField(
    val name: String = "",
    /** HTML. */
    val value: String = "",
    @SerialName("verified_at")
    @Serializable(with = LenientInstantSerializer::class)
    val verifiedAt: Instant? = null,
)

/** Relationship entity — follow state between the logged user and an account. */
@Serializable
data class Relationship(
    val id: String,
    val following: Boolean = false,
    @SerialName("followed_by") val followedBy: Boolean = false,
    val requested: Boolean = false,
    val blocking: Boolean = false,
    val muting: Boolean = false,
)
