package com.gigapingu.neon.core.data

import android.util.Log
import com.gigapingu.neon.core.data.di.ApplicationScope
import com.gigapingu.neon.core.model.MastoNotification
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.network.ApiClient
import com.gigapingu.neon.core.network.StreamingClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class StreamEnvelope(val event: String, val payload: String? = null)

/**
 * Live updates via the Mastodon `user` streaming WebSocket — home timeline
 * (new/edited/deleted posts) + notifications, multiplexed on one connection.
 * Active only while the app is foregrounded and the user is authenticated
 * ([setForeground] combined with [AuthRepository.status]); backgrounded
 * delivery stays on the existing FCM push path.
 */
@Singleton
class StreamingRepository @Inject constructor(
    private val streamingClient: StreamingClient,
    private val api: ApiClient,
    private val authRepository: AuthRepository,
    private val instanceRepository: InstanceRepository,
    private val timelineRepository: TimelineRepository,
    private val notificationRepository: NotificationRepository,
    private val json: Json,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private companion object {
        const val TAG = "StreamingRepository"
        val MIN_BACKOFF = 2.seconds
        val MAX_BACKOFF = 30.seconds
    }

    private val foreground = MutableStateFlow(false)
    private var connectionJob: Job? = null

    init {
        scope.launch {
            combine(authRepository.status, foreground) { status, isForeground ->
                status == AuthStatus.Authenticated && isForeground
            }.distinctUntilChanged().collect { active ->
                if (active) startConnectionLoop() else stopConnectionLoop()
            }
        }
    }

    fun setForeground(active: Boolean) {
        foreground.value = active
    }

    private fun startConnectionLoop() {
        stopConnectionLoop()
        connectionJob = scope.launch {
            var backoff = MIN_BACKOFF
            while (isActive) {
                val url = resolveStreamingUrl()
                val token = api.token
                if (url != null && token != null) {
                    try {
                        streamingClient.connect(url, token).collect(::handle)
                        backoff = MIN_BACKOFF
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Streaming connection failed, retrying in $backoff", e)
                        backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF)
                    }
                } else {
                    // No streaming host advertised (or token momentarily unset) — back off instead of spinning.
                    backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF)
                }
                delay(backoff)
            }
        }
    }

    private fun stopConnectionLoop() {
        connectionJob?.cancel()
        connectionJob = null
    }

    private suspend fun resolveStreamingUrl(): String? {
        val host = runCatching { instanceRepository.getInstance().streamingUrl }.getOrNull() ?: return null
        return "$host/api/v1/streaming/?stream=user"
    }

    private fun handle(raw: String) {
        val envelope = decode(StreamEnvelope.serializer(), raw) ?: return
        val payload = envelope.payload
        when (envelope.event) {
            "update" -> payload?.let { decode(Status.serializer(), it) }
                ?.let(timelineRepository::prependCreated)
            "status.update" -> payload?.let { decode(Status.serializer(), it) }?.let { status ->
                timelineRepository.applyStatusUpdate(status)
                notificationRepository.applyStatusUpdate(status)
            }
            "delete" -> payload?.let { id ->
                timelineRepository.applyStatusDelete(id)
                notificationRepository.applyStatusDelete(id)
            }
            "notification" -> payload?.let { decode(MastoNotification.serializer(), it) }
                ?.let(notificationRepository::prependNotification)
            else -> Unit // filters_changed / conversation / announcement* — not handled yet
        }
    }

    private fun <T> decode(serializer: KSerializer<T>, raw: String): T? =
        runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
}
