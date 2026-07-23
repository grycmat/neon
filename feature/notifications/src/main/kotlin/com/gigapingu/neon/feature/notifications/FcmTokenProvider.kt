package com.gigapingu.neon.feature.notifications

import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Suspending wrapper around FirebaseMessaging's token Task. Returns null on failure. */
@Singleton
class FcmTokenProvider @Inject constructor() {
    suspend fun getToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            cont.resume(if (task.isSuccessful) task.result else null)
        }
    }
}
