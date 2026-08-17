package com.gigapingu.neon.feature.notifications

import com.gigapingu.neon.core.data.push.PushDistributorStatus
import com.gigapingu.neon.core.data.push.PushEndpointProvider
import com.google.firebase.messaging.FirebaseMessaging
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * gms flavor's [PushEndpointProvider]: wraps the FCM registration token and points it at the
 * self-hosted mastodon-fcm-relay, which forwards the still-encrypted payload over FCM.
 */
@Singleton
class GmsPushEndpointProvider @Inject constructor() : PushEndpointProvider {
    override suspend fun getEndpoint(): String? = getToken()?.let { buildEndpoint(it) }

    override fun getDistributorStatus(): PushDistributorStatus =
        PushDistributorStatus.Available("Google Play Services")

    /** Also used by NeonFirebaseMessagingService.onNewToken to re-register on token rotation. */
    fun buildEndpoint(token: String): String =
        BuildConfig.RELAY_BASE_URL.trimEnd('/') + "/push/" + URLEncoder.encode(token, "UTF-8")

    private suspend fun getToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            cont.resume(if (task.isSuccessful) task.result else null)
        }
    }
}
