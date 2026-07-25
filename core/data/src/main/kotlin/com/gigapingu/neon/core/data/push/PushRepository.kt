package com.gigapingu.neon.core.data.push

import com.gigapingu.neon.core.data.NeonConfig
import com.gigapingu.neon.core.network.ApiClient
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Registers / removes the Mastodon Web Push subscription that points at the
 * mastodon-fcm-relay. The device's FCM token is embedded in the endpoint path so
 * the relay knows which device to forward each push to. The public key + auth
 * secret come from [PushKeyManager]; the matching private key stays on-device for
 * [WebPushDecryptor].
 */
@Singleton
class PushRepository @Inject constructor(
    private val api: ApiClient,
    private val keyManager: PushKeyManager,
    private val json: Json,
) {
    @Volatile
    private var lastRegistrationKey: String? = null

    /** POST /api/v1/push/subscription. No-op if not authenticated or unchanged since the last call. */
    suspend fun register(fcmToken: String, alerts: NotificationAlertPrefs = NotificationAlertPrefs()) {
        if (!api.isConfigured) return
        val registrationKey = "$fcmToken:$alerts"
        if (registrationKey == lastRegistrationKey) return

        val keys = keyManager.getOrCreateKeys()
        val endpoint = NeonConfig.RELAY_BASE_URL.trimEnd('/') +
            "/push/" + URLEncoder.encode(fcmToken, "UTF-8")
        val request = RegisterPushRequest(
            subscription = PushSubscriptionBody(
                endpoint = endpoint,
                keys = PushKeysBody(p256dh = keys.p256dhBase64, auth = keys.authBase64),
            ),
            data = PushDataBody(alerts = alerts.toAlertsBody()),
        )
        api.post("/api/v1/push/subscription", json.encodeToString(RegisterPushRequest.serializer(), request))
        lastRegistrationKey = registrationKey
    }

    /** DELETE /api/v1/push/subscription. Best-effort (a missing subscription is fine). */
    suspend fun unregister() {
        lastRegistrationKey = null
        if (!api.isConfigured) return
        runCatching { api.delete("/api/v1/push/subscription") }
    }
}

/** Per-notification-type push alert toggles (Settings > Notifications). */
data class NotificationAlertPrefs(
    val mention: Boolean = true,
    val favourite: Boolean = true,
    val reblog: Boolean = true,
    val follow: Boolean = true,
    val followRequest: Boolean = true,
    val poll: Boolean = true,
    val status: Boolean = true,
    val update: Boolean = true,
)

private fun NotificationAlertPrefs.toAlertsBody() = PushAlertsBody(
    mention = mention,
    favourite = favourite,
    reblog = reblog,
    follow = follow,
    follow_request = followRequest,
    poll = poll,
    status = status,
    update = update,
)

// Mastodon accepts these as nested JSON, mapping to the bracketed form params
// (subscription[keys][p256dh], data[alerts][mention], ...). See docs.joinmastodon.org/methods/push/.

@Serializable
private data class PushKeysBody(
    val p256dh: String,
    val auth: String,
)

@Serializable
private data class PushSubscriptionBody(
    val endpoint: String,
    val keys: PushKeysBody,
    // RFC 8291 aes128gcm; requires Mastodon >= 4.4 to honor. Older instances fall
    // back to legacy aesgcm, which WebPushDecryptor also handles.
    val standard: Boolean = true,
)

@Serializable
private data class PushAlertsBody(
    val mention: Boolean = true,
    val favourite: Boolean = true,
    val reblog: Boolean = true,
    val follow: Boolean = true,
    val follow_request: Boolean = true,
    val poll: Boolean = true,
    val status: Boolean = true,
    val update: Boolean = true,
)

@Serializable
private data class PushDataBody(
    val alerts: PushAlertsBody = PushAlertsBody(),
    val policy: String = "all",
)

@Serializable
private data class RegisterPushRequest(
    val subscription: PushSubscriptionBody,
    val data: PushDataBody = PushDataBody(),
)
