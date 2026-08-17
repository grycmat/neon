package com.gigapingu.neon.feature.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.gigapingu.neon.core.data.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Legacy GCM/C2DM broadcast receiver, registered alongside [NeonFirebaseMessagingService] for
 * the exact same messages. This mirrors how the official Mastodon Android app
 * (org.joinmastodon.android, see PushNotificationReceiver.java) receives push: a manifest
 * BroadcastReceiver for a signature-permission-protected system broadcast is exempt from the
 * Android 8+ background-service-start limits that a FirebaseMessagingService wake-up is subject
 * to, and observationally (Samsung's `BaseRestrictionMgr`/Freecess logs) gets waved through where
 * a Service start for a rarely-used app does not. Play Services still sends this broadcast for
 * backward compatibility with apps that never migrated off the old GCM API.
 *
 * The data payload arrives as plain string extras — the same keys as [RemoteMessage.getData]
 * (`body`, `contentEncoding`, `encryption`, `cryptoKey`) since that's just the message's `data`
 * map, not a Firebase-specific encoding.
 */
@AndroidEntryPoint
class NeonC2dmReceiver : BroadcastReceiver() {

    @Inject lateinit var handler: PushMessageHandler
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.google.android.c2dm.intent.RECEIVE") return

        val extras = intent.extras ?: return
        val data = extras.keySet().mapNotNull { key ->
            extras.getString(key)?.let { key to it }
        }.toMap()

        Log.d(NEON_PUSH_TAG, "onReceive (c2dm): data keys=${data.keys}")

        val pendingResult = goAsync()
        scope.launch {
            try {
                handler.handle(data, fallbackId = System.currentTimeMillis().toString())
            } finally {
                pendingResult.finish()
            }
        }
    }
}
