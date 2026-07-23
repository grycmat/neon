package com.gigapingu.neon.feature.notifications

import android.util.Log
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.data.AuthStatus
import com.gigapingu.neon.core.data.SettingsRepository
import com.gigapingu.neon.core.data.di.ApplicationScope
import com.gigapingu.neon.core.data.push.PushRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var handler: PushMessageHandler

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
        Log.d(
            NEON_PUSH_TAG,
            "onMessageReceived: data keys=${message.data.keys} " +
                "priority=${message.priority} originalPriority=${message.originalPriority}",
        )
        val fallbackId = message.messageId ?: System.currentTimeMillis().toString()
        scope.launch {
            handler.handle(message.data, fallbackId)
        }
    }
}
