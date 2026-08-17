package com.gigapingu.neon.feature.notifications

import android.util.Log
import com.gigapingu.neon.core.data.AuthRepository
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

/**
 * Receives FCM data-only messages forwarded by the mastodon-fcm-relay, decrypts the
 * Web Push payload on-device ([WebPushDecryptor]), and posts a system notification.
 * Also re-registers the relay subscription when the FCM token rotates.
 */
@AndroidEntryPoint
class NeonFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var pushRepository: PushRepository
    @Inject lateinit var pushEndpointProvider: GmsPushEndpointProvider
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var handler: PushMessageHandler

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onNewToken(token: String) {
        scope.launch {
            try {
                val endpoint = pushEndpointProvider.buildEndpoint(token)
                val alerts = settingsRepository.notificationAlertPrefs.first()
                if (pushRepository.registerIfEligible(endpoint, alerts, authRepository, settingsRepository)) {
                    Log.i(NEON_PUSH_TAG, "Re-registered push subscription after token refresh")
                }
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
