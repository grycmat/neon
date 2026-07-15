package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.model.MediaAttachment
import com.gigapingu.neon.core.network.ApiClient
import com.gigapingu.neon.core.network.FilePart
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Media uploads for the composer (POST /api/v2/media). */
@Singleton
class MediaRepository @Inject constructor(
    private val api: ApiClient,
    private val json: Json,
) {
    suspend fun upload(
        bytes: ByteArray,
        filename: String,
        description: String? = null,
    ): MediaAttachment = json.decodeFromString(
        MediaAttachment.serializer(),
        api.multipart(
            method = "POST",
            path = "/api/v2/media",
            fields = buildMap {
                if (!description.isNullOrEmpty()) put("description", description)
            },
            files = listOf(FilePart(name = "file", filename = filename, bytes = bytes)),
        ),
    )

    suspend fun updateDescription(id: String, description: String): MediaAttachment =
        json.decodeFromString(
            MediaAttachment.serializer(),
            api.put(
                "/api/v1/media/$id",
                buildJsonObject { put("description", description) }.toString(),
            ),
        )
}
