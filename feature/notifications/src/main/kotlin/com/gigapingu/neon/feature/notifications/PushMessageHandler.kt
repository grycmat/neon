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
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.data.NotificationRepository
import com.gigapingu.neon.core.data.NotificationWidgetBridge
import com.gigapingu.neon.core.data.SettingsRepository
import com.gigapingu.neon.core.data.push.PushKeyManager
import com.gigapingu.neon.core.data.push.WebPushDecryptor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Log tag for push registration/delivery diagnostics. */
const val NEON_PUSH_TAG = "NeonPush"

/** Notification channel id shared between channel creation (NeonApplication) and posting. */
const val NEON_NOTIFICATION_CHANNEL_ID = "neon_notifications"

/**
 * Shared decrypt-and-display logic for an incoming Web Push payload, regardless of which
 * Android entry point delivered it — FCM/C2DM ([NeonFirebaseMessagingService]/[NeonC2dmReceiver],
 * gms flavor) or UnifiedPush (NeonPushService, foss flavor). Lives in the flavor-agnostic
 * source set since every delivery entry point across both flavors depends on it.
 */
class PushMessageHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pushKeyManager: PushKeyManager,
    private val decryptor: WebPushDecryptor,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
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
        if (settingsRepository.notificationsEnabled.first()) {
            postNotification(body, contentEncoding, data, fallbackId)
        } else {
            Log.w(NEON_PUSH_TAG, "Dropping push: notifications disabled in settings")
        }

        // Last, and not gated on that setting: turning off notification *alerts* is not a reason
        // to let the home-screen widget go stale. This is the widget's live update path while the
        // app is backgrounded (streaming covers the foreground). Awaited rather than launched, so
        // the caller's BroadcastReceiver PendingResult keeps the process alive for the fetch —
        // and after the notification is posted, so that round-trip never delays what the user sees.
        NotificationWidgetBridge.refresh()
    }

    private suspend fun postNotification(
        body: String,
        contentEncoding: String,
        data: Map<String, String>,
        fallbackId: String,
    ) {
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
        // ensureConfigured matters here — a push often lands in a process the system started for
        // the broadcast, where AuthRepository.restore never ran and ApiClient has no host/token,
        // so this lookup would fail on every backgrounded delivery.
        val statusId = runCatching {
            if (!authRepository.ensureConfigured()) return@runCatching null
            val full = notificationRepository.getNotification(notificationId)
            full.status?.display?.id
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
