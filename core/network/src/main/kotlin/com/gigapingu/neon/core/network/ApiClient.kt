package com.gigapingu.neon.core.network

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class ApiException(val statusCode: Int, override val message: String) : IOException(message) {
    override fun toString(): String = "API $statusCode: $message"
}

/** A named binary part for multipart requests (media upload, profile images). */
data class FilePart(val name: String, val filename: String, val bytes: ByteArray)

/**
 * Thin HTTP wrapper bound to the current instance + access token.
 * AuthRepository configures it; every other repository consumes it.
 * Returns raw JSON strings — repositories decode with kotlinx.serialization.
 */
@Singleton
class ApiClient @Inject constructor(private val client: OkHttpClient) {

    @Volatile private var instanceHost: String? = null
    @Volatile private var token: String? = null

    val isConfigured: Boolean get() = instanceHost != null
    val instance: String? get() = instanceHost

    fun configure(instance: String, token: String? = null) {
        this.instanceHost = instance
        this.token = token
    }

    fun setToken(token: String?) {
        this.token = token
    }

    fun reset() {
        instanceHost = null
        token = null
    }

    fun buildUrl(path: String, query: Map<String, Any?> = emptyMap()): HttpUrl {
        val host = checkNotNull(instanceHost) { "ApiClient not configured" }
        val builder = HttpUrl.Builder()
            .scheme("https")
            .host(host)
            .addPathSegments(path.removePrefix("/"))
        query.forEach { (key, value) ->
            when (value) {
                null -> Unit
                is List<*> -> value.forEach { builder.addQueryParameter(key, it.toString()) }
                else -> builder.addQueryParameter(key, value.toString())
            }
        }
        return builder.build()
    }

    suspend fun get(path: String, query: Map<String, Any?> = emptyMap()): String =
        execute(Request.Builder().url(buildUrl(path, query)).get())

    suspend fun post(path: String, body: String = "{}"): String =
        execute(jsonRequest(path).post(body.toRequestBody(JSON_MEDIA_TYPE)))

    suspend fun put(path: String, body: String = "{}"): String =
        execute(jsonRequest(path).put(body.toRequestBody(JSON_MEDIA_TYPE)))

    suspend fun delete(path: String): String =
        execute(Request.Builder().url(buildUrl(path)).delete())

    /** Multipart request — media upload (POST) and profile edits (PATCH). */
    suspend fun multipart(
        method: String,
        path: String,
        fields: Map<String, String> = emptyMap(),
        files: List<FilePart> = emptyList(),
    ): String {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
            fields.forEach { (name, value) -> addFormDataPart(name, value) }
            files.forEach { part ->
                addFormDataPart(
                    part.name,
                    part.filename,
                    part.bytes.toRequestBody("application/octet-stream".toMediaType()),
                )
            }
        }.build()
        return execute(Request.Builder().url(buildUrl(path)).method(method, body))
    }

    private fun jsonRequest(path: String): Request.Builder =
        Request.Builder().url(buildUrl(path))

    private suspend fun execute(builder: Request.Builder): String {
        val request = builder
            .header("Accept", "application/json")
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .build()
        val response = client.newCall(request).await()
        return response.use { res ->
            val body = res.body?.string().orEmpty()
            if (res.isSuccessful) {
                body
            } else {
                throw ApiException(res.code, extractError(body))
            }
        }
    }

    private fun extractError(body: String): String {
        // Try `error_description` / `error` without a full model.
        val regexes = listOf(
            Regex("\"error_description\"\\s*:\\s*\"([^\"]*)\""),
            Regex("\"error\"\\s*:\\s*\"([^\"]*)\""),
        )
        for (regex in regexes) {
            regex.find(body)?.groupValues?.get(1)?.let { return it }
        }
        return body.ifEmpty { "Request failed" }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

/** OkHttp Call → coroutine, with cancellation propagation. */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }

        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) continuation.resumeWithException(e)
        }
    })
    continuation.invokeOnCancellation { runCatching { cancel() } }
}
