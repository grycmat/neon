package com.gigapingu.neon.core.model

import java.time.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Status entity (https://docs.joinmastodon.org/entities/Status/). */
@Serializable
data class Status(
    val id: String,
    val uri: String = "",
    val url: String? = null,
    val account: Account,
    /** HTML. */
    val content: String = "",
    @SerialName("created_at")
    @Serializable(with = LenientInstantSerializer::class)
    val createdAt: Instant? = null,
    /** public | unlisted | private | direct */
    val visibility: String = "public",
    @SerialName("spoiler_text") val spoilerText: String = "",
    @SerialName("replies_count") val repliesCount: Int = 0,
    @SerialName("reblogs_count") val reblogsCount: Int = 0,
    @SerialName("favourites_count") val favouritesCount: Int = 0,
    val favourited: Boolean = false,
    val reblogged: Boolean = false,
    val bookmarked: Boolean = false,
    @SerialName("in_reply_to_id") val inReplyToId: String? = null,
    @SerialName("in_reply_to_account_id") val inReplyToAccountId: String? = null,
    /** Set when this status is a boost. */
    val reblog: Status? = null,
    /**
     * Native quote (Mastodon 4.4+). The wire format is a wrapper
     * `{"state": "accepted", "quoted_status": {...}}` — [QuoteUnwrapSerializer]
     * flattens it to the quoted status (or null when absent/shallow).
     */
    @Serializable(with = QuoteUnwrapSerializer::class)
    val quote: Status? = null,
    @SerialName("media_attachments") val mediaAttachments: List<MediaAttachment> = emptyList(),
    val poll: Poll? = null,
    val mentions: List<StatusMention> = emptyList(),
    val tags: List<StatusTag> = emptyList(),
    val card: PreviewCard? = null,
) {
    /** The status to render (unwraps boosts). */
    val display: Status get() = reblog ?: this
    val isBoost: Boolean get() = reblog != null
    val shareUrl: String get() = url?.takeIf { it.isNotEmpty() } ?: uri
}

@Serializable
data class StatusMention(
    val id: String = "",
    val username: String = "",
    val acct: String = "",
    val url: String = "",
)

@Serializable
data class StatusTag(
    val name: String = "",
    val url: String = "",
)

@Serializable
data class PreviewCard(
    val url: String = "",
    val title: String = "",
    val description: String = "",
    val image: String? = null,
)

/** GET /api/v1/statuses/:id/context — thread ancestors + descendants. */
@Serializable
data class StatusContext(
    val ancestors: List<Status> = emptyList(),
    val descendants: List<Status> = emptyList(),
)

/**
 * Handles the Mastodon 4.4 quote wrapper on the wire while the model keeps a
 * plain `Status?`:
 *  - `{"quoted_status": {...}}` → the inner status
 *  - a bare status object (older servers) → itself
 *  - shallow quotes (id string) / rejected quotes → null
 * Serializes back to the wrapper form so cached JSON stays wire-compatible.
 */
object QuoteUnwrapSerializer : KSerializer<Status?> {
    private val delegate: KSerializer<Status?> by lazy { Status.serializer().nullable }

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor by lazy {
        SerialDescriptor("neon.Quote", delegate.descriptor)
    }

    override fun deserialize(decoder: Decoder): Status? {
        val input = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
        val element = input.decodeJsonElement()
        val obj = element as? JsonObject ?: return null
        val quoted = (obj["quoted_status"] as? JsonObject) ?: obj
        if (quoted["id"] == null || quoted["id"] is JsonNull) return null
        return runCatching {
            input.json.decodeFromJsonElement(Status.serializer(), quoted)
        }.getOrNull()
    }

    override fun serialize(encoder: Encoder, value: Status?) {
        val output = encoder as? JsonEncoder ?: run { delegate.serialize(encoder, value); return }
        if (value == null) {
            output.encodeJsonElement(JsonNull)
        } else {
            output.encodeJsonElement(buildJsonObject {
                put("state", "accepted")
                put("quoted_status", output.json.encodeToJsonElement(Status.serializer(), value))
            })
        }
    }
}
