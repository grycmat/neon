package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.model.Poll
import com.gigapingu.neon.core.model.PollDraft
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.model.StatusContext
import com.gigapingu.neon.core.model.StatusSource
import com.gigapingu.neon.core.network.ApiClient
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * All status interactions. After every mutation it directly patches the
 * singleton list repositories (timelines, notifications) and notifies any
 * registered [StatusListener]s (screen ViewModels holding their own lists)
 * so state stays in sync across screens.
 */
@Singleton
class StatusRepository @Inject constructor(
    private val api: ApiClient,
    private val json: Json,
    private val timelines: TimelineRepository,
    private val notifications: NotificationRepository,
    private val bookmarks: BookmarkRepository,
) {
    /** Implemented by ViewModels that hold status lists (thread, profile). */
    interface StatusListener {
        fun onStatusUpdated(status: Status) {}
        fun onStatusCreated(status: Status) {}
        fun onPollUpdated(poll: Poll) {}
        fun onStatusDeleted(id: String) {}
    }

    private val listeners = CopyOnWriteArrayList<StatusListener>()

    fun addListener(listener: StatusListener) {
        listeners += listener
    }

    fun removeListener(listener: StatusListener) {
        listeners -= listener
    }

    suspend fun getStatus(id: String): Status =
        json.decodeFromString(Status.serializer(), api.get("/api/v1/statuses/$id"))

    suspend fun getSource(id: String): StatusSource =
        json.decodeFromString(StatusSource.serializer(), api.get("/api/v1/statuses/$id/source"))

    suspend fun getContext(id: String): StatusContext =
        json.decodeFromString(StatusContext.serializer(), api.get("/api/v1/statuses/$id/context"))

    suspend fun favourite(status: Status): Status =
        toggle(status, if (status.favourited) "unfavourite" else "favourite")

    suspend fun reblog(status: Status): Status =
        toggle(status, if (status.reblogged) "unreblog" else "reblog")

    suspend fun bookmark(status: Status): Status =
        toggle(status, if (status.bookmarked) "unbookmark" else "bookmark")

    private suspend fun toggle(status: Status, action: String): Status {
        var updated = json.decodeFromString(
            Status.serializer(),
            api.post("/api/v1/statuses/${status.id}/$action"),
        )
        // reblog returns the wrapping boost — unwrap to the target status.
        if (action == "reblog" && updated.reblog != null) updated = updated.reblog!!
        timelines.applyStatusUpdate(updated)
        notifications.applyStatusUpdate(updated)
        bookmarks.applyStatusUpdate(updated)
        listeners.forEach { it.onStatusUpdated(updated) }
        return updated
    }

    /** Votes on a poll. The endpoint returns only the updated Poll. */
    suspend fun vote(poll: Poll, choices: List<Int>): Poll {
        val body = buildJsonObject {
            putJsonArray("choices") { choices.forEach { add(it) } }
        }
        val updated = json.decodeFromString(
            Poll.serializer(),
            api.post("/api/v1/polls/${poll.id}/votes", body.toString()),
        )
        timelines.applyPollUpdate(updated)
        bookmarks.applyPollUpdate(updated)
        listeners.forEach { it.onPollUpdated(updated) }
        return updated
    }

    /** Creates a status: plain post, reply, quote, with media and/or a poll. */
    suspend fun create(
        text: String,
        visibility: String = "public",
        inReplyToId: String? = null,
        quotedStatusId: String? = null,
        spoilerText: String? = null,
        mediaIds: List<String> = emptyList(),
        poll: PollDraft? = null,
    ): Status {
        val body = buildJsonObject {
            put("status", text)
            put("visibility", visibility)
            inReplyToId?.let { put("in_reply_to_id", it) }
            quotedStatusId?.let { put("quoted_status_id", it) }
            if (!spoilerText.isNullOrEmpty()) {
                put("spoiler_text", spoilerText)
                put("sensitive", true)
            }
            if (mediaIds.isNotEmpty()) {
                putJsonArray("media_ids") { mediaIds.forEach { add(it) } }
            }
            poll?.let {
                putJsonObject("poll") {
                    putJsonArray("options") { it.options.forEach { option -> add(option) } }
                    put("expires_in", it.expiresInSeconds)
                    put("multiple", it.multiple)
                }
            }
        }
        val status = json.decodeFromString(
            Status.serializer(),
            api.post("/api/v1/statuses", body.toString()),
        )
        timelines.prependCreated(status)
        listeners.forEach { it.onStatusCreated(status) }
        return status
    }

    /** Edits a status. The endpoint returns the updated Status. */
    suspend fun edit(
        id: String,
        text: String,
        visibility: String = "public",
        spoilerText: String? = null,
        mediaIds: List<String> = emptyList(),
        poll: PollDraft? = null,
    ): Status {
        val body = buildJsonObject {
            put("status", text)
            put("visibility", visibility)
            if (!spoilerText.isNullOrEmpty()) {
                put("spoiler_text", spoilerText)
                put("sensitive", true)
            } else {
                put("spoiler_text", "")
                put("sensitive", false)
            }
            if (mediaIds.isNotEmpty()) {
                putJsonArray("media_ids") { mediaIds.forEach { add(it) } }
            } else {
                putJsonArray("media_ids")
            }
            poll?.let {
                putJsonObject("poll") {
                    putJsonArray("options") { it.options.forEach { option -> add(option) } }
                    put("expires_in", it.expiresInSeconds)
                    put("multiple", it.multiple)
                }
            }
        }
        val status = json.decodeFromString(
            Status.serializer(),
            api.put("/api/v1/statuses/$id", body.toString()),
        )
        timelines.applyStatusUpdate(status)
        notifications.applyStatusUpdate(status)
        bookmarks.applyStatusUpdate(status)
        listeners.forEach { it.onStatusUpdated(status) }
        return status
    }

    suspend fun delete(id: String) {
        api.delete("/api/v1/statuses/$id")
        timelines.applyStatusDelete(id)
        notifications.applyStatusDelete(id)
        bookmarks.applyStatusDelete(id)
        listeners.forEach { it.onStatusDeleted(id) }
    }
}
