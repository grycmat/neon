package com.gigapingu.neon.feature.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
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
        val pubKeySet = endpoint.pubKeySet ?: return
        serviceScope.launch {
            try {
                notificationRepository.registerPushWithInstance(
                    endpointUrl = endpoint.url,
                    p256dh = pubKeySet.pubKey,
                    auth = pubKeySet.auth
                )
            } catch (e: Exception) {
                // Log registration failure
            }
        }
    }

    override fun onMessage(message: PushMessage, instance: String) {
        if (message.decrypted) {
            val decryptedJson = String(message.content, Charsets.UTF_8)
            try {
                val payload = json.decodeFromString(MastodonPushPayload.serializer(), decryptedJson)
                showNotification(payload)
            } catch (e: Exception) {
                // Log parsing/notification display failure
            }
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

            val appIconRes = context.packageManager.getApplicationInfo(context.packageName, 0).icon

            val builder = NotificationCompat.Builder(context, "neon_notifications")
                .setSmallIcon(appIconRes)
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

    override fun onUnregistered(instance: String) {}

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {}

    override fun onTempUnavailable(instance: String) {}
}
