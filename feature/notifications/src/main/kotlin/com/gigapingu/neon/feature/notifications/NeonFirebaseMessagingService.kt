package com.gigapingu.neon.feature.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.data.AuthStatus
import com.gigapingu.neon.core.data.NotificationRepository
import com.gigapingu.neon.core.data.SettingsRepository
import com.gigapingu.neon.core.data.di.ApplicationScope
import com.gigapingu.neon.core.data.push.PushKeyManager
import com.gigapingu.neon.core.data.push.PushRepository
import com.gigapingu.neon.core.data.push.WebPushDecryptor
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Log tag for push registration/delivery diagnostics. */
const val NEON_PUSH_TAG = "NeonPush"

/** Notification channel id shared between channel creation (NeonApplication) and posting. */
const val NEON_NOTIFICATION_CHANNEL_ID = "neon_notifications"

/**
 * Receives FCM data-only messages forwarded by the mastodon-fcm-relay, decrypts the
 * Web Push payload on-device ([WebPushDecryptor]), and posts a system notification.
 * Also re-registers the relay subscription when the FCM token rotates.
 */
@AndroidEntryPoint
class NeonFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var pushRepository: PushRepository
    @Inject lateinit var pushKeyManager: PushKeyManager
    @Inject lateinit var decryptor: WebPushDecryptor
    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var json: Json

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onNewToken(token: String) {
        scope.launch {
            try {
                if (authRepository.status.value != AuthStatus.Authenticated) return@launch
                if (!settingsRepository.notificationsEnabled.first()) return@launch
                pushRepository.register(token)
                Log.i(NEON_PUSH_TAG, "Re-registered push subscription after token refresh")
            } catch (e: Exception) {
                Log.e(NEON_PUSH_TAG, "Failed to re-register after token refresh", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.i(NEON_PUSH_TAG, "onMessageReceived: data keys=${message.data.keys}")
        val data = message.data
        val body = data["body"]
        val contentEncoding = data["contentEncoding"]
        if (body.isNullOrEmpty() || contentEncoding.isNullOrEmpty()) {
            Log.w(NEON_PUSH_TAG, "Dropping push: missing body/contentEncoding")
            return
        }
        scope.launch {
            if (!settingsRepository.notificationsEnabled.first()) {
                Log.w(NEON_PUSH_TAG, "Dropping push: notifications disabled in settings")
                return@launch
            }
            try {
                val keys = pushKeyManager.getOrCreateKeys()
                val plaintext = decryptor.decrypt(
                    bodyBase64 = body,
                    contentEncoding = contentEncoding,
                    encryption = data["encryption"],
                    cryptoKey = data["cryptoKey"],
                    keys = keys,
                )
                val payload = json.parseToJsonElement(String(plaintext, Charsets.UTF_8)).jsonObject
                Log.i(NEON_PUSH_TAG, "Decrypted push payload: $payload")
                // Mastodon sends notification_id as a JSON number on many versions
                // (mastodon#32749) — .content handles both number and string forms.
                val notificationId = payload["notification_id"]?.jsonPrimitive?.content
                if (notificationId == null) {
                    Log.w(NEON_PUSH_TAG, "Dropping push: no notification_id in payload")
                    return@launch
                }
                val title = payload["title"]?.jsonPrimitive?.content ?: "Neon"
                val text = payload["body"]?.jsonPrimitive?.content.orEmpty()
                showNotification(notificationId, title, text)
            } catch (e: Exception) {
                Log.e(NEON_PUSH_TAG, "Failed to decrypt/display push", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun showNotification(notificationId: String, title: String, text: String) {
        // Best-effort: resolve the underlying status so the tap can deep-link to the thread.
        val statusId = runCatching {
            val full = notificationRepository.getNotification(notificationId)
            full.status?.id ?: full.status?.reblog?.id
        }.getOrNull()

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            statusId?.let { putExtra("status_id", it) }
            putExtra("open_notifications", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, NEON_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_neon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(notificationId.hashCode(), notification)
            Log.i(NEON_PUSH_TAG, "Notification posted: id=$notificationId title=$title")
        } else {
            Log.w(NEON_PUSH_TAG, "Dropping push: POST_NOTIFICATIONS permission not granted")
        }
    }
}
