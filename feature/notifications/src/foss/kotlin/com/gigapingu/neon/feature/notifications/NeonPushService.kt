package com.gigapingu.neon.feature.notifications

import android.util.Base64
import android.util.Log
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.data.SettingsRepository
import com.gigapingu.neon.core.data.di.ApplicationScope
import com.gigapingu.neon.core.data.push.PushRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

/**
 * UnifiedPush entry point for the foss flavor — extends the connector library's [PushService].
 * Incoming distributor broadcasts (new endpoint, message, unregister, failed) are captured by the
 * connector's internal receiver and delivered to this service via `PUSH_EVENT`.
 *
 * Forwarding decrypted bytes to the existing [PushMessageHandler] mirrors how the gms entry points
 * handle incoming pushes.
 */
@AndroidEntryPoint
class NeonPushService : PushService() {

    @Inject lateinit var pushRepository: PushRepository
    @Inject lateinit var pushEndpointProvider: UnifiedPushEndpointProvider
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var handler: PushMessageHandler
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        Log.i(NEON_PUSH_TAG, "onNewEndpoint received from UnifiedPush distributor: ${endpoint.url}")
        UnifiedPushEndpointStore.save(applicationContext, endpoint.url)
        scope.launch {
            try {
                val alerts = settingsRepository.notificationAlertPrefs.first()
                if (pushRepository.registerIfEligible(endpoint.url, alerts, authRepository, settingsRepository)) {
                    Log.i(NEON_PUSH_TAG, "Registered UnifiedPush endpoint with Mastodon")
                }
            } catch (e: Exception) {
                Log.e(NEON_PUSH_TAG, "Failed to register UnifiedPush endpoint", e)
            } finally {
                pushEndpointProvider.markRegistrationSettled()
            }
        }
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        Log.e(NEON_PUSH_TAG, "UnifiedPush registration failed: $reason")
        pushEndpointProvider.markRegistrationSettled()
    }

    override fun onTempUnavailable(instance: String) {
        Log.w(NEON_PUSH_TAG, "UnifiedPush distributor temporarily unavailable")
        pushEndpointProvider.markRegistrationSettled()
    }

    override fun onUnregistered(instance: String) {
        Log.i(NEON_PUSH_TAG, "UnifiedPush unregistered for instance: $instance")
        UnifiedPushEndpointStore.clear(applicationContext)
        pushEndpointProvider.markRegistrationSettled()
        scope.launch { pushRepository.unregister() }
    }

    override fun onMessage(message: PushMessage, instance: String) {
        Log.i(NEON_PUSH_TAG, "UnifiedPush message received: length=${message.content.size} decrypted=${message.decrypted}")
        if (message.decrypted) {
            Log.w(NEON_PUSH_TAG, "Dropping push the connector claims to have decrypted itself")
            return
        }
        val data = mapOf(
            "body" to Base64.encodeToString(message.content, Base64.NO_WRAP),
            "contentEncoding" to "aes128gcm",
        )
        scope.launch {
            handler.handle(data, fallbackId = System.currentTimeMillis().toString())
        }
    }
}
