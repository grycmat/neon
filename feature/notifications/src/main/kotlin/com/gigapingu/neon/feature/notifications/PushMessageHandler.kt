package com.gigapingu.neon.feature.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gigapingu.neon.core.data.NotificationRepository
import com.gigapingu.neon.core.data.SettingsRepository
import com.gigapingu.neon.core.data.push.PushKeyManager
import com.gigapingu.neon.core.data.push.WebPushDecryptor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Shared decrypt-and-display logic for an incoming Web Push payload, regardless of which
 * Android entry point delivered it — the modern [NeonFirebaseMessagingService] or the legacy
 * C2DM [NeonC2dmReceiver] (mirroring how the official Mastodon app receives pushes; some OEMs
 * treat a manifest-registered broadcast receiver far more leniently than a Service wake-up for
 * a backgrounded app, see CLAUDE.md Push notifications section).
 */
class PushMessageHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pushKeyManager: PushKeyManager,
    private val decryptor: WebPushDecryptor,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository,
    private val json: Json,
) {

    /** @param fallbackId used when the decrypted payload has no notification_id, or decryption fails. */
    suspend fun handle(data: Map<String, String>, fallbackId: String) {
        val body = data["body"]
        val contentEncoding = data["contentEncoding"]
        if (body.isNullOrEmpty() || contentEncoding.isNullOrEmpty()) {
            Log.w(NEON_PUSH_TAG, "Dropping push: missing body/contentEncoding")
            return
        }
        if (!settingsRepository.notificationsEnabled.first()) {
            Log.w(NEON_PUSH_TAG, "Dropping push: notifications disabled in settings")
            return
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
            val notificationId = payload["notification_id"]?.jsonPrimitive?.content ?: fallbackId
            val title = payload["title"]?.jsonPrimitive?.content ?: "Neon"
            val text = payload["body"]?.jsonPrimitive?.content.orEmpty()
            showNotification(notificationId, title, text)
        } catch (e: Exception) {
            // Always show *some* notification for a high-priority push we accepted:
            // FCM silently downgrades an app's future high-priority messages to normal
            // priority (delayed delivery) if they repeatedly wake the app without producing
            // a visible notification. A decrypt failure here is usually a stale keypair
            // (e.g. after a reinstall regenerated the on-device key but the server's
            // subscription still points at the old one) — surfacing a fallback keeps push
            // reliable and gives a visible signal that re-subscribing is needed.
            Log.e(NEON_PUSH_TAG, "Failed to decrypt/display push, showing fallback", e)
            showNotification(fallbackId, "Neon", "You have a new notification. Tap to open.")
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun showNotification(notificationId: String, title: String, text: String) {
        // Best-effort: resolve the underlying status so the tap can deep-link to the thread.
        val statusId = runCatching {
            val full = notificationRepository.getNotification(notificationId)
            full.status?.id ?: full.status?.reblog?.id
        }.getOrNull()

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            statusId?.let { putExtra("status_id", it) }
            putExtra("open_notifications", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NEON_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_neon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(notificationId.hashCode(), notification)
            Log.i(NEON_PUSH_TAG, "Notification posted: id=$notificationId title=$title")
        } else {
            Log.w(NEON_PUSH_TAG, "Dropping push: POST_NOTIFICATIONS permission not granted")
        }
    }
}
