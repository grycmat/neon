package com.gigapingu.neon.feature.notifications

import android.content.Context
import android.util.Log
import com.gigapingu.neon.core.data.push.PushEndpointProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.unifiedpush.android.connector.UnifiedPush
import org.unifiedpush.android.connector.data.ResolvedDistributor

/**
 * foss flavor's [PushEndpointProvider]: the distributor's own endpoint is itself a fully
 * RFC 8291-compliant Web Push endpoint, so Mastodon posts encrypted payloads straight to it —
 * no relay involved.
 *
 * Returns the last endpoint [NeonUnifiedPushReceiver] was handed, after confirming the saved
 * distributor is still installed (a distributor uninstalled outside the app never gets a chance
 * to send `onUnregistered`, so a stale cached endpoint would otherwise persist forever). If no
 * live endpoint exists, resolves the distributor via [UnifiedPush.resolveDefaultDistributor]
 * (Context-only, no Activity/OS picker needed when there's an unambiguous single distributor)
 * and requests registration, returning null — [NeonUnifiedPushReceiver.onNewEndpoint] completes
 * the actual `PushRepository.register` call once the distributor responds, mirroring how the gms
 * flavor's `onNewToken` re-registers on FCM rotation. When multiple distributors are installed
 * with none chosen yet ([ResolvedDistributor.ToSelect]), or none are installed at all
 * ([ResolvedDistributor.NoneAvailable]), this deliberately does not surface a picker UI — out of
 * scope for now, see degoogle.md.
 */
@Singleton
class UnifiedPushEndpointProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : PushEndpointProvider {

    /**
     * Guards against re-sending a registration request on every call while one is already
     * pending (e.g. repeated ON_RESUME before the distributor has responded). Reset by
     * [markRegistrationSettled] once [NeonUnifiedPushReceiver] observes a terminal outcome
     * (new endpoint, registration failure, or unregistration) for the attempt this started.
     */
    private val registrationInFlight = AtomicBoolean(false)

    // getSavedDistributor/getDistributors/resolveDefaultDistributor are synchronous PackageManager
    // / Binder calls, not suspend functions — this is called from ShellViewModel.syncPushRegistration
    // on viewModelScope (Main), so it needs the same off-Main treatment CLAUDE.md documents for
    // ApiClient's response read, or every ON_RESUME blocks the UI thread on a Binder round-trip.
    override suspend fun getEndpoint(): String? = withContext(Dispatchers.IO) {
        cachedEndpointIfLive()?.let { return@withContext it }
        requestRegistrationIfIdle()
        null
    }

    /** Called by [NeonUnifiedPushReceiver] once a registration attempt reaches a terminal state. */
    fun markRegistrationSettled() {
        registrationInFlight.set(false)
    }

    /**
     * The stored endpoint, or null if there is none or its distributor is no longer installed —
     * an uninstall never gets a chance to send `onUnregistered`, so a dead endpoint would
     * otherwise be cached forever.
     */
    private fun cachedEndpointIfLive(): String? {
        val stored = UnifiedPushEndpointStore.get(context) ?: return null
        val savedDistributor = UnifiedPush.getSavedDistributor(context)
        if (savedDistributor != null && savedDistributor in UnifiedPush.getDistributors(context)) {
            return stored
        }
        Log.w(NEON_PUSH_TAG, "Saved UnifiedPush distributor is gone — clearing stale endpoint")
        UnifiedPushEndpointStore.clear(context)
        return null
    }

    /**
     * Resolves a distributor and asks it to register, unless a request from an earlier call is
     * still pending. Only the [ResolvedDistributor.Found] path leaves [registrationInFlight] set
     * — it's cleared by [markRegistrationSettled] once [NeonUnifiedPushReceiver] observes a
     * terminal outcome for the request this starts. Every other path, including a thrown
     * exception (e.g. a malfunctioning distributor), must clear it itself here, or no callback
     * will ever arrive to do so and every future call would short-circuit to null forever.
     */
    private fun requestRegistrationIfIdle() {
        if (!registrationInFlight.compareAndSet(false, true)) return

        try {
            when (val resolved = UnifiedPush.resolveDefaultDistributor(context)) {
                is ResolvedDistributor.Found -> {
                    UnifiedPush.saveDistributor(context, resolved.packageName)
                    UnifiedPush.register(context)
                }
                is ResolvedDistributor.ToSelect -> {
                    Log.w(NEON_PUSH_TAG, "Multiple UnifiedPush distributors installed, none chosen yet")
                    registrationInFlight.set(false)
                }
                is ResolvedDistributor.NoneAvailable -> {
                    Log.w(NEON_PUSH_TAG, "No UnifiedPush distributor installed — push unavailable")
                    registrationInFlight.set(false)
                }
            }
        } catch (e: Exception) {
            Log.e(NEON_PUSH_TAG, "Failed to resolve/register a UnifiedPush distributor", e)
            registrationInFlight.set(false)
        }
    }
}
