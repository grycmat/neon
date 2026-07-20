package com.gigapingu.neon.feature.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gigapingu.neon.core.data.NotificationRepository
import com.gigapingu.neon.core.data.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage
import javax.inject.Inject

/** Log tag for push registration/delivery diagnostics. */
const val NEON_PUSH_TAG = "NeonPush"

/** Notification channel id shared between channel creation and posting. */
const val NEON_NOTIFICATION_CHANNEL_ID = "neon_notifications"

@Serializable
data class MastodonPushPayload(
    @SerialName("notification_id") val notificationId: String,
    @SerialName("notification_type") val notificationType: String,
    val title: String,
    val body: String,
    val icon: String? = null
)

@AndroidEntryPoint
class NeonPushService : PushService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var json: Json

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        val pubKeySet = endpoint.pubKeySet
        if (pubKeySet == null) {
            Log.w(NEON_PUSH_TAG, "onNewEndpoint without pubKeySet — connector webpush keys missing")
            return
        }
        serviceScope.launch {
            try {
                notificationRepository.registerPushWithInstance(
                    endpointUrl = endpoint.url,
                    p256dh = pubKeySet.pubKey,
                    auth = pubKeySet.auth
                )
                Log.i(NEON_PUSH_TAG, "Registered push subscription with instance")
            } catch (e: Exception) {
                Log.e(NEON_PUSH_TAG, "Failed to register push subscription with instance", e)
            }
        }
    }

    override fun onMessage(message: PushMessage, instance: String) {
        if (!message.decrypted) {
            // Connector couldn't decrypt: usually a legacy-encryption mismatch
            // (subscription[standard] not honored / instance < 4.4) or key drift.
            Log.w(NEON_PUSH_TAG, "Dropping push message: not decrypted by connector")
            return
        }
        val decryptedJson = String(message.content, Charsets.UTF_8)
        try {
            val payload = json.decodeFromString(MastodonPushPayload.serializer(), decryptedJson)
            showNotification(payload)
        } catch (e: Exception) {
            Log.e(NEON_PUSH_TAG, "Failed to parse/display push payload", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(payload: MastodonPushPayload) {
        serviceScope.launch {
            val notificationsEnabled = settingsRepository.notificationsEnabled.first()
            if (!notificationsEnabled) return@launch

            var statusId: String? = null
            try {
                val fullNotification = notificationRepository.getNotification(payload.notificationId)
                statusId = fullNotification.status?.id ?: fullNotification.status?.reblog?.id
            } catch (e: Exception) {
                // Fallback to opening notifications feed on fetch failure
                Log.w(NEON_PUSH_TAG, "Could not resolve status for notification ${payload.notificationId}", e)
            }

            val context = this@NeonPushService
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (statusId != null) {
                    putExtra("status_id", statusId)
                }
                putExtra("open_notifications", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                payload.notificationId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, NEON_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_neon)
                .setContentTitle(payload.title)
                .setContentText(payload.body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val manager = NotificationManagerCompat.from(context)
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                manager.notify(payload.notificationId.hashCode(), builder.build())
            }
        }
    }

    override fun onUnregistered(instance: String) {
        Log.i(NEON_PUSH_TAG, "Unregistered from distributor")
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        Log.e(NEON_PUSH_TAG, "Push registration failed: $reason")
    }

    override fun onTempUnavailable(instance: String) {
        Log.w(NEON_PUSH_TAG, "Push temporarily unavailable")
    }
}
