package com.gigapingu.neon.feature.notifications

import android.content.Context
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
import org.unifiedpush.android.connector.MessagingReceiver
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

/**
 * UnifiedPush entry point for the foss flavor — a manifest [MessagingReceiver], mirroring why
 * [NeonC2dmReceiver] (gms flavor) is a broadcast receiver rather than a service: it survives OEM
 * background restrictions better than a Service wake-up for a backgrounded app. Unlike
 * [NeonC2dmReceiver]'s underlying system broadcast, [MessagingReceiver.onReceive] doesn't call
 * `goAsync()` itself, so every callback below that launches async work must call it directly —
 * otherwise the process is free to be killed (and the connector's own wake lock, held only for
 * the duration of `onReceive`, is released) before [PushMessageHandler.handle] finishes.
 *
 * The connector's own decryption is unused: Mastodon encrypts to this app's `PushKeyManager`
 * keypair (registered via [PushRepository], same as the gms flavor), not any key the connector
 * manages, so [PushMessage.content] is treated as opaque ciphertext — [onMessage] defensively
 * drops the rare case where the connector's `decrypted` flag comes back true anyway, since that
 * would mean `content` is no longer the aes128gcm ciphertext [PushMessageHandler] expects.
 */
@AndroidEntryPoint
class NeonUnifiedPushReceiver : MessagingReceiver() {

    @Inject lateinit var pushRepository: PushRepository
    @Inject lateinit var pushEndpointProvider: UnifiedPushEndpointProvider
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var handler: PushMessageHandler
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onNewEndpoint(context: Context, endpoint: PushEndpoint, instance: String) {
        UnifiedPushEndpointStore.save(context, endpoint.url)
        goAsyncLaunch {
            try {
                val alerts = settingsRepository.notificationAlertPrefs.first()
                if (pushRepository.registerIfEligible(endpoint.url, alerts, authRepository, settingsRepository)) {
                    Log.i(NEON_PUSH_TAG, "Registered UnifiedPush endpoint")
                }
            } catch (e: Exception) {
                Log.e(NEON_PUSH_TAG, "Failed to register UnifiedPush endpoint", e)
            } finally {
                pushEndpointProvider.markRegistrationSettled()
            }
        }
    }

    override fun onRegistrationFailed(context: Context, reason: FailedReason, instance: String) {
        Log.e(NEON_PUSH_TAG, "UnifiedPush registration failed: $reason")
        pushEndpointProvider.markRegistrationSettled()
    }

    override fun onTempUnavailable(context: Context, instance: String) {
        Log.w(NEON_PUSH_TAG, "UnifiedPush distributor temporarily unavailable")
        // A terminal outcome for whatever registration attempt is in flight (if any) — without
        // this, a distributor that never recovers to send onNewEndpoint would leave
        // registrationInFlight stuck forever, permanently blocking every future getEndpoint().
        pushEndpointProvider.markRegistrationSettled()
    }

    override fun onUnregistered(context: Context, instance: String) {
        UnifiedPushEndpointStore.clear(context)
        pushEndpointProvider.markRegistrationSettled()
        goAsyncLaunch { pushRepository.unregister() }
    }

    override fun onMessage(context: Context, message: PushMessage, instance: String) {
        if (message.decrypted) {
            // The connector attempts decryption with its own internally-managed key, unrelated
            // to the PushKeyManager keypair Mastodon actually encrypts to — this should never
            // succeed for a real Mastodon push. Don't feed already-"decrypted" bytes back through
            // WebPushDecryptor as if they were still aes128gcm ciphertext.
            Log.w(NEON_PUSH_TAG, "Dropping push the connector claims to have decrypted itself")
            return
        }
        // Always aes128gcm, unlike the gms path (which forwards whatever contentEncoding the
        // relay reported): PushMessage exposes only the raw body, never the separate Encryption/
        // Crypto-Key headers legacy aesgcm decryption needs (see WebPushDecryptor), and those
        // can't be recovered after the fact. A Mastodon instance older than 4.4 — which ignores
        // the `standard=true` PushRepository requests and always sends aesgcm — can't be
        // supported over UnifiedPush at all; its pushes will consistently fail to decrypt and
        // fall back to a generic notification. The gms flavor doesn't have this limitation.
        val data = mapOf(
            "body" to Base64.encodeToString(message.content, Base64.NO_WRAP),
            "contentEncoding" to "aes128gcm",
        )
        goAsyncLaunch {
            handler.handle(data, fallbackId = System.currentTimeMillis().toString())
        }
    }

    /**
     * [MessagingReceiver.onReceive] doesn't call `goAsync()` itself (see class doc), so every
     * callback that does async work needs the same pending-result dance — extracted here since
     * three of the four abstract overrides above need it verbatim.
     */
    private fun goAsyncLaunch(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                block()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
