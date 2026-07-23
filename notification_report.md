# Why Neon's push notifications weren't arriving (and what fixed it)

## TL;DR

Neon posted push notifications through a modern `FirebaseMessagingService`.
On this Samsung device, waking a **Service** for a backgrounded, rarely-used
app is subject to background-execution limits that Samsung's proprietary
`Freecess` process manager enforces even more aggressively than stock
Android — and it enforces them *silently*: no error to the relay, no crash,
no log line on-device, nothing. The FCM backend reported `201 Sent` every
time; the message simply never reached app code.

The official Mastodon app doesn't use `FirebaseMessagingService` at all. It
still uses the legacy **GCM/C2DM `BroadcastReceiver`** API from a decade ago.
A broadcast to a signature-permission-protected receiver is a fundamentally
lighter-weight, more privileged wake path than starting a Service, and it is
exempt from the restrictions that were silently eating Neon's messages.

The fix was to add that same legacy `BroadcastReceiver` to Neon, side by side
with the existing `FirebaseMessagingService`, so Play Services has the same
privileged path available that it already uses for the official app. No
server-side change, no loss of the on-device-decryption/zero-trust relay
design — same architecture, second delivery door.

## The investigation, in order

Everything below was tested and eliminated, one at a time, before we found
the actual cause. Recording this because most of it *looked* like it should
have been the answer.

| # | Theory | How we tested it | Result |
|---|---|---|---|
| 1 | Relay/FCM send failing | `journalctl -u mastodon-fcm-relay` | Relay always got `201 Sent` from Firebase Admin SDK — ruled out |
| 2 | App-level bug (manifest, `google-services.json`, applicationId mismatch) | Read `AndroidManifest.xml`, `google-services.json`, `app/build.gradle.kts` | All correctly wired — ruled out |
| 3 | Samsung "Sleeping apps" / Device Care restriction list | Manually checked — Neon wasn't on it | Ruled out |
| 4 | Standard Android battery optimization (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` / "Unrestricted" toggle) | Set to Unrestricted manually | No change — ruled out as a *sufficient* fix |
| 5 | FCM's own 7-day high-priority message reliability throttle (Android 13+ silently downgrades an app's high-priority messages to normal priority if they don't reliably result in a shown notification) | Added `priority`/`originalPriority` logging; did a full uninstall + reinstall + fresh login (new Firebase Installation ID resets the tracking window) | Still nothing, even from a completely clean install — ruled out |
| 6 | Stale Web Push subscription (subscribed with an old keypair from a previous install) | Fresh install + fresh login produces a new keypair *and* a fresh `/api/v1/push/subscription` call; confirmed via `journalctl` — brand-new FCM token, clean `201 Sent` | Subscription was live and current — ruled out |
| 7 | **Real cause**: OS/OEM-level delivery of the message to the app process | Unfiltered `adb logcat` around the exact moment of a test push | Found it — see below |

## The evidence that nailed it

An unfiltered logcat capture (not grepped to our own log tag) during a test
push showed this repeating, for Neon specifically, the whole time:

```
FreecessHandler: skipping freeze com.gigapingu.neon(10592) result : 12
```

`Freecess` is Samsung's own background-process management daemon — a third,
largely undocumented layer *underneath* the Device Care UI (distinct from
both the "Sleeping apps" list and the standard Android battery-optimization
toggle, both of which we'd already ruled out).

The decisive line came moments later, in the same capture, for the
**official Mastodon app**, while its notification was delivered instantly:

```
BaseRestrictionMgr: Package: org.joinmastodon.android, userid: 0,
hostingType: broadcast is allowed by freecess, caller is: com.google.android.gms
```

Two things stood out:

1. Samsung's restriction manager explicitly evaluated and **allowed** a wake
   for `org.joinmastodon.android`, and logged `hostingType: broadcast`.
2. **No equivalent line — allow or deny — ever appeared for `com.gigapingu.neon`,
   in any capture.** GMS wasn't being blocked waking Neon. It wasn't even
   asking the restriction manager. The request never got that far.

That `hostingType: broadcast` was the key detail. It meant the official app
wakes up via a **broadcast**, not a **service start**. Pulling its source
confirmed it:

```java
// mastodon-android: PushNotificationReceiver.java
public class PushNotificationReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        byte[] rawData = intent.getByteArrayExtra("rawData");
        // ...decrypt on-device, same as Neon's WebPushDecryptor...
    }
}
```

```xml
<!-- mastodon-android: AndroidManifest.xml -->
<receiver android:name=".PushNotificationReceiver"
          android:exported="true"
          android:permission="com.google.android.gms.permission.SEND">
  <intent-filter>
    <action android:name="com.google.android.c2dm.intent.RECEIVE" />
  </intent-filter>
</receiver>
```

No `FirebaseMessagingService` anywhere in that app. It never migrated off
the original 2013-era GCM/C2DM API — and that turned out to be the whole
advantage.

## Why a broadcast gets through and a Service doesn't

Since Android 8 (API 26), apps in the background cannot be freely started as
a `Service` by another app — this is the "background execution limits"
change that Doze, App Standby Buckets, and every OEM's own battery manager
build on top of. Waking a `FirebaseMessagingService` for a backgrounded app
means Play Services has to start a Service on your behalf, which is squarely
inside those limits.

A `BroadcastReceiver` registered in the manifest for a **signature-permission-
protected system broadcast** (`com.google.android.c2dm.permission.SEND`,
held only by Play Services / GMS) is treated differently by the platform —
this is the same category of exemption that lets, e.g., SMS-received
broadcasts fire for backgrounded apps. Samsung's `Freecess` layer inherits
that distinction: it evaluates and allows the broadcast path, and — based on
what we observed — doesn't even get consulted for the Service-start path,
which appears to be getting silently dropped further upstream.

This is also why none of the earlier fixes worked: `IGNORE_BATTERY_OPTIMIZATIONS`,
the Device Care sleeping-apps list, and the FCM 7-day throttle all operate
*after* the OS has decided to attempt a wake. Our problem was upstream of
all of them — the wake attempt for a Service was never being made in a way
that reached any of those checkpoints.

## Before → after

### Architecture, before

One entry point. A data-only FCM message could only reach Neon through
`FirebaseMessagingService.onMessageReceived`:

```kotlin
// feature/notifications/.../NeonFirebaseMessagingService.kt (before)
@AndroidEntryPoint
class NeonFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var pushKeyManager: PushKeyManager
    @Inject lateinit var decryptor: WebPushDecryptor
    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    // ...

    override fun onMessageReceived(message: RemoteMessage) {
        // decrypt + build + post the notification, all inline here
    }

    private suspend fun showNotification(id: String, title: String, text: String) {
        // NotificationCompat.Builder(...).build() + NotificationManagerCompat.notify(...)
    }
}
```

```xml
<!-- AndroidManifest.xml (before) -->
<service
    android:name="com.gigapingu.neon.feature.notifications.NeonFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.messaging.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

Single delivery path, and it happened to be the one path this device's OS
was silently dropping.

### Architecture, after

The decrypt/notify logic moved into a shared, injectable `PushMessageHandler`
so it's identical no matter which entry point runs it:

```kotlin
// feature/notifications/.../PushMessageHandler.kt (new)
class PushMessageHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pushKeyManager: PushKeyManager,
    private val decryptor: WebPushDecryptor,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository,
    private val json: Json,
) {
    suspend fun handle(data: Map<String, String>, fallbackId: String) {
        // same decrypt + build + post logic as before, entry-point-agnostic
    }
}
```

`NeonFirebaseMessagingService` shrank to a thin adapter:

```kotlin
// feature/notifications/.../NeonFirebaseMessagingService.kt (after)
@AndroidEntryPoint
class NeonFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var handler: PushMessageHandler
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onMessageReceived(message: RemoteMessage) {
        val fallbackId = message.messageId ?: System.currentTimeMillis().toString()
        scope.launch { handler.handle(message.data, fallbackId) }
    }
}
```

And a second entry point was added, mirroring the official app's receiver
exactly, sharing the same handler:

```kotlin
// feature/notifications/.../NeonC2dmReceiver.kt (new)
@AndroidEntryPoint
class NeonC2dmReceiver : BroadcastReceiver() {
    @Inject lateinit var handler: PushMessageHandler
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.google.android.c2dm.intent.RECEIVE") return
        val extras = intent.extras ?: return
        val data = extras.keySet().mapNotNull { key -> extras.getString(key)?.let { key to it } }.toMap()

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
```

```xml
<!-- AndroidManifest.xml (after) — added alongside the existing <service> -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<permission
    android:name="${applicationId}.permission.C2D_MESSAGE"
    android:protectionLevel="signature" />
<uses-permission android:name="${applicationId}.permission.C2D_MESSAGE" />
<uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />

<receiver
    android:name="com.gigapingu.neon.feature.notifications.NeonC2dmReceiver"
    android:exported="true"
    android:permission="com.google.android.c2dm.permission.SEND">
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE" />
    </intent-filter>
</receiver>
```

Nothing about the *relay* or the *encryption* changed. The server still
never sees plaintext; the client still decrypts the same aes128gcm Web Push
payload on-device. The only change is that there are now **two** manifest
entry points Play Services can use to hand Neon that payload — the modern
one, and the same legacy one the official app relies on — instead of one.

### Why this is flawless now

Whichever path Play Services actually uses on a given device/OS version,
Neon now has a route through a privileged, exemption-eligible wake mechanism
identical to a client that's known to work reliably on this exact device. If
both paths ever fire for the same message, it's harmless — `handle()` calls
`NotificationManagerCompat.notify()` with the same notification ID derived
from the same decrypted `notification_id`, so a duplicate delivery just
overwrites itself rather than showing twice.

## Lessons for next time

- **"The server got a 200/201" only means the message was accepted by FCM's
  backend for delivery — it is not proof of delivery.** There is no
  supported way to get a delivery receipt back to the sender for a message
  that silently never arrives; you have to instrument the *device* side to
  find out.
- **OEM battery/process management is not one setting.** On this Samsung
  device there were at least three independent layers stacked on top of
  stock Android Doze: the Device Care "Sleeping apps" list, the standard
  per-app battery-usage toggle, and the undocumented `Freecess` daemon. Each
  had to be checked and ruled out separately; none of the user-facing
  toggles controlled `Freecess`.
- **Filter logs by package/subsystem, not by your own tag, when something is
  silently not happening.** Every earlier capture was grepped for our own
  `NeonPush` tag or FCM keywords and came back empty — which felt like
  confirmation of "nothing arrived" but was really confirmation of "nothing
  from *our own code* ran." The unfiltered capture is what surfaced
  `FreecessHandler` and `BaseRestrictionMgr`, neither of which we would have
  thought to search for in advance.
- **When a reference implementation works and yours doesn't, on the same
  device, under the same restrictions, diff the *mechanism*, not just the
  crypto/protocol.** Neon and the official app were doing functionally
  identical Web Push decryption. The difference that mattered was invisible
  at that layer — it was entirely in *which OS primitive* (`Service` vs
  `BroadcastReceiver`) each one used to wake up.
- **Mirroring a known-working implementation exactly beat reasoning about
  Android's restriction stack from first principles.** We had several
  theories (battery optimization, FCM throttling, stale keys) that were each
  individually plausible and each individually wrong. What actually worked
  was replicating the working reference implementation's delivery mechanism
  byte-for-byte, permission-for-permission.
