package com.gigapingu.neon.core.network

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Raw WebSocket transport for the Mastodon streaming API. Returns text frames
 * as-is — same "raw string, caller decodes" convention as [ApiClient] — since
 * kotlinx.serialization isn't wired into this module.
 */
@Singleton
class StreamingClient @Inject constructor(private val client: OkHttpClient) {

    /**
     * Opens a WebSocket to [url] (a full `wss://...` URL string) and emits each
     * text frame received. Completes when the socket closes; throws if it fails.
     * Closing the socket happens automatically when the collecting coroutine is cancelled.
     */
    fun connect(url: String, token: String): Flow<String> = callbackFlow {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()
        val streamingClient = client.newBuilder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
        val webSocket = streamingClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    trySend(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    close(t)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    channel.close()
                }
            },
        )
        awaitClose { webSocket.close(1000, null) }
    }
}
