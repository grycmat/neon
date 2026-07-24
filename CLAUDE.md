# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Neon is a native Android Mastodon client — a Kotlin + Jetpack Compose port of a
Flutter app (`../flutter`), matching the "glassy pink→purple→cyan" design in
`Neon Mastodon Client.html`. When behavior is ambiguous, the Flutter sibling
project is the reference implementation to check against.

Stack: Kotlin 2.2, JVM 17, AGP 8.11, compileSdk 36 / minSdk 26, Jetpack Compose
(Material 3), Navigation 3 (pre-1.0, see caveats below), Hilt for DI, Room for
the offline cache, OkHttp + kotlinx.serialization for the Mastodon REST API
(no Retrofit — instance host is dynamic, chosen at login), Coil 3 for images,
DataStore for credentials/settings.

## Build & run

- Open the repo root in Android Studio (Narwhal or newer) and let it sync — this is the normal workflow.
- From the CLI (the Gradle wrapper is committed):
  - `./gradlew :app:assembleDebug` — build the debug APK
  - `./gradlew :app:installDebug` — build and install on a connected device/emulator
  - `./gradlew build` — full build of all modules
  - `./gradlew :core:data:build` (etc.) — build a single module
- A clean checkout builds and runs with **no secrets**: OAuth app registration
  happens dynamically against whatever instance the user enters at login; the
  redirect `neon://oauth` is intercepted inside an in-app WebView, so no manifest
  intent-filter/scheme is needed. Defaults (redirect URI, scopes, default
  instance) live in `core/data/.../NeonConfig.kt`.
- **Push notifications** (see Architecture below) need real config, gitignored:
  - `google-services.json` at the app module root for Firebase/FCM. Without it
    the `com.google.gms.google-services` Gradle plugin fails, so a build that
    doesn't touch Firebase is unaffected but installing/running with push is.
  - `secrets.properties` at the repo root with `RELAY_BASE_URL` (the deployed
    `mastodon-fcm-relay` host). Copy `secrets.properties.example`. It is read in
    `core/data/build.gradle.kts` into `BuildConfig.RELAY_BASE_URL`; absent, it
    falls back to `RELAY_BASE_URL` env var, then `https://relay.example.com`
    (builds fine, push just won't deliver).
- There is currently no automated test suite in this repo.

## Architecture

### Module graph

```
app                   Auth gate, Navigation 3 wiring, HomeShell (swipeable tabs + TopAppBar + FAB), ShellViewModel
core/model            API entities (Status, Account, Poll, Notification, …)
core/network          ApiClient (OkHttp wrapper bound to instance + token)
core/database          Room cache (list_cache / entity_cache tables)
core/data             Repositories: Auth, Timeline, Status, Notification, Account, Bookmark, Media, Search, Settings;
                      push/ (Web Push subscription + on-device decryption, see Push notifications)
core/designsystem     NeonPalette/NeonTheme/typography, Glass* components, NeonBackground, HtmlText
core/ui               StatusCard, MediaGrid, PollView, QuoteCard, StatusActions, AccountRow, AsyncList,
                      MediaPreviewScreen (full-screen viewer), PreviewFixtures,
                      Navigator + StatusActionService singletons (and the NavKeys)
feature/auth          Login + in-app OAuth WebView
feature/timeline      Home / Local / Federated with segmented pills
feature/explore       Trends + search (also pushed for hashtag taps)
feature/notifications Notifications feed; NeonFirebaseMessagingService + NeonC2dmReceiver +
                      PushMessageHandler + FcmTokenProvider (push)
feature/thread        Thread view (ancestors → focused → replies)
feature/composer      Composer: media + alt text, polls, CW, visibility, @-autocomplete
feature/profile       Profile, follow lists, edit profile
feature/settings      Theme mode + logout
```

`core/*` modules have no dependency on `feature/*` or `app`; `feature/*`
modules depend on `core/*` but not on each other — cross-feature navigation
goes through the `Navigator` singleton (below), not direct module deps.

### State pattern: singleton repositories + StateFlow

Every list-backed screen is driven by a `@Singleton` repository (in
`core/data`) exposing `StateFlow<AsyncState<T>>` (`AsyncState` in
`AsyncState.kt`: `Idle / Loading / LoadingMore / Refreshing / Ready / Error`
phases plus `hasMore` for pagination). ViewModels collect this state directly
rather than owning their own copies — the repository is the source of truth,
not the ViewModel.

### Cross-screen sync via direct calls

`StatusRepository` is the hub for all status interactions (favourite, boost,
vote, create, delete). After every mutation it syncs the other list holders
directly — no event bus:
- it calls `TimelineRepository.applyStatusUpdate` / `applyPollUpdate` /
  `prependCreated` and `NotificationRepository.applyStatusUpdate` (both are
  injected singletons), and
- it notifies registered `StatusRepository.StatusListener`s — implemented by
  `ThreadViewModel` and `ProfileViewModel`, which `addListener(this)` in
  `init` and `removeListener(this)` in `onCleared()` (several can be alive at
  once because Nav3 keeps a ViewModel per back-stack entry).

All receivers patch their cached lists in place with `patchStatusList` /
`patchPollList` (`StatusListPatch.kt`), which also follow into
boosted/reblogged statuses. This is how a favourite/boost/vote made in one
screen (e.g. a thread) shows up immediately in another (e.g. the home
timeline) without a refetch. When adding a new mutation or list screen, wire
it into this direct-call/listener pattern.

### Offline cache

`CacheStore` (`core/data/CacheStore.kt`) is a typed facade over `core/database`'s
Room DAO. Lists are cached as `list_cache` rows keyed by `(listKey, position)`
storing raw entity JSON; single entities go in `entity_cache` keyed by an
entity key. This mirrors the Flutter app's sqflite cache design intentionally,
for cache-first rendering. Decode failures are swallowed (`runCatching { ... }.getOrNull()`)
so a schema/model change never bricks startup — never make cache reads throw.

### Networking

`ApiClient` (`core/network/ApiClient.kt`) is a thin OkHttp wrapper bound at
runtime to whichever instance host + token the user authenticated with —
there is no Retrofit and no compile-time base URL. Repositories build request
bodies manually with `kotlinx.serialization`'s `buildJsonObject` DSL and parse
responses with per-model `KSerializer`s, rather than generating API interfaces.

### Push notifications

Delivered over **FCM data messages** relayed through a self-hosted
`mastodon-fcm-relay`, with **all decryption on-device** — the relay never sees
plaintext. The device subscribes to Mastodon Web Push (RFC 8030/8188/8291)
pointing the endpoint at the relay, which forwards each still-encrypted payload
via FCM. Pieces:
- `core/data/push/PushKeyManager` — generates + persists the P-256 ECDH keypair
  and 16-byte auth secret in `EncryptedSharedPreferences`. Only the public key +
  auth secret ever leave the device.
- `core/data/push/PushRepository` — `POST/DELETE /api/v1/push/subscription`. The
  FCM token is URL-encoded into the endpoint path (`RELAY_BASE_URL/push/<token>`)
  so the relay knows which device to forward to. Registers with `standard=true`
  (aes128gcm; Mastodon ≥ 4.4).
- `core/data/push/WebPushDecryptor` — pure crypto, no Firebase/network; decrypts
  both modern `aes128gcm` and legacy `aesgcm` payloads.
- `feature/notifications/FcmTokenProvider` — suspending wrapper over the FCM token
  Task. `PushMessageHandler` holds the shared decrypt-and-post logic (decrypts,
  resolves the status best-effort to deep-link, posts to the `neon_notifications`
  channel created in `NeonApplication.onCreate`) and is called from **two**
  entry points, both registered in `app/AndroidManifest.xml`:
  - `NeonFirebaseMessagingService` (`FirebaseMessagingService.onMessageReceived`) — the
    modern path.
  - `NeonC2dmReceiver` — a manifest `BroadcastReceiver` for the legacy
    `com.google.android.c2dm.intent.RECEIVE` system broadcast, deliberately mirroring
    how the official `org.joinmastodon.android` app receives push. This exists because
    some OEMs (observed: Samsung's `Freecess`/`BaseRestrictionMgr`) silently drop a
    `Service` wake-up for a backgrounded/rarely-used app well before Android's own
    Doze/App-Standby checks ever run, with no error surfaced anywhere, while a
    broadcast to a signature-permission-protected receiver (`com.google.android.c2dm.permission.SEND`,
    requires the matching `<permission>`/`<uses-permission>` block for
    `${applicationId}.permission.C2D_MESSAGE`) is exempt from those limits. Both
    entry points can fire for the same message; that's harmless since
    `PushMessageHandler` always resolves the same `notification_id` and
    `NotificationManagerCompat.notify()` on a duplicate id just overwrites in place.
    Full investigation writeup: `notification_report.md`.
- **Sync loop**: `ShellViewModel.syncPushRegistration(hasPermission)` is the single
  entry point, called from a `MainActivity` `LaunchedEffect` keyed on auth status,
  the `notificationsEnabled` setting, and `POST_NOTIFICATIONS` permission (re-checked
  on `ON_RESUME`). It registers when all three hold, else unregisters;
  `PushRepository` de-dupes redundant re-registration by last token. `AuthRepository`
  logout unregisters (while the token is still valid) then wipes the keypair.
- Notification taps route through `Navigator.handleNotificationClick` (via
  `MainActivity.handleNotificationIntent` on `status_id` / `open_notifications`
  extras).

### Navigation

Built on **Navigation 3** (`androidx.navigation3`, still pre-1.0 — see below),
wired in `app/src/main/kotlin/com/gigapingu/neon/NeonApp.kt`:
- Routes are serializable `NavKey`s (`core/ui/.../Navigator.kt`), pushed onto
  a `NavBackStack` via `entryProvider { entry<SomeKey> { ... } }`. Screen
  transitions are set globally on `NavDisplay` in `NeonApp.kt`: pushes slide in
  right-to-left (old screen parallaxes left); pops — button and predictive back
  gesture alike — play the exact mirror, sliding out left-to-right
  (`android:enableOnBackInvokedCallback="true"` is set in the app manifest so
  the gesture drives the pop animation). One per-entry exception: `ComposeKey`
  overrides via `NavDisplay.transitionSpec`/`popTransitionSpec`/
  `predictivePopTransitionSpec` metadata to expand out of the compose FAB's
  bottom-end corner (scale + fade from a fixed `TransformOrigin`, not a true
  shared-element transition) and collapse back into it on pop. Don't add
  further per-entry transition metadata or shared-element/hero animations.
- `NeonApp` first gates on `ShellViewModel.authStatus` (Unknown / Unauthenticated
  / Authenticated) before mounting the real nav graph.
- Navigation and status actions are **plain singleton `object`s in `core/ui`**,
  called directly from any composable (no CompositionLocals, no interfaces):
  - `Navigator` holds `var backStack: NavBackStack?` — `NeonApp` binds it in a
    `DisposableEffect` while the authenticated shell is on screen; while null
    (previews, login) every call no-ops. Screens call `Navigator.openThread(id)`,
    `Navigator.back()`, etc.
  - `StatusActionService` (favourite/boost/vote/share/open-mention) is
    initialized from `NeonApplication.onCreate` with Hilt-injected repos; it
    owns a Main-dispatcher scope, shows failures as Toasts, and resolves
    mention taps straight to `Navigator.openProfile`.
- `HomeShell` hosts the four root tabs (Home / Explore / Notifications /
  Profile) in a `HorizontalPager` with `beyondViewportPageCount = 3` so tab
  state survives swiping, and draws the shared glassmorphic top app bar itself
  — tab screens must not add their own headers or `statusBarsPadding`
  (`ProfileScreen` pads conditionally because it is also pushed standalone).

### Big screens (unfolded foldables / tablets)

`core/ui/.../BigScreen.kt` is the whole adaptive vocabulary — no adaptive
library: `isBigScreen()` (window width ≥ 640dp, re-reads on fold/unfold),
`hingePaneWidth(inShell)` (left-pane width so the pane divider lands on the
window centre = the hinge; `inShell` subtracts the nav rail), and
`PaneSelection` (gradient edge marker on the list row open in a detail pane).
Past the threshold, from the "Neon Foldable" design:
- `HomeShell` swaps the bottom tab bar + FAB for a left nav rail (`ShellRail`)
  and turns the Home and Notifications tabs into list-detail
  (`ShellListDetail`, composed in `app` because list and detail are different
  feature modules): `Navigator.threadPaneHandler` — bound by HomeShell while
  it is on screen, same pattern as `Navigator.backStack` — reroutes
  `openThread` from the visible list tab into an embedded
  `ThreadScreen(embedded = true)` right pane instead of pushing.
- Pushed screens adapt themselves: `ThreadScreen` goes focus mode (toot left
  of the hinge, replies + reply bar right), `ExploreScreen` and
  `ProfileScreen` split at the hinge, `FollowListScreen` chunks into two
  columns, `ComposeScreen` becomes a centered 620dp dialog, `SettingsScreen`
  caps its content width. Phone layouts are untouched below the threshold.
- Not ported from the design (intentional): the two-column federated feed,
  the quote popover (bottom sheet stays), and a persistent rail under pushed
  screens (pushes cover the full window, keeping Nav3's global transitions).

### Motion

`NeonMotion` (`core/designsystem/.../theme/NeonMotion.kt`) is the motion
vocabulary for **in-screen feedback only**: `quick()` tweens for short fades
(titles, counters, pane crossfades), `bouncy()` springs for icon pops and
pressed states, `screen()` for larger in-screen reveals (poll bars, boost
spin). Screen-to-screen transitions are the global slide/predictive-back specs
on `NavDisplay` (see Navigation above), not part of `NeonMotion`.

### Compose previews & stateless screens

Screens are split into a stateful ViewModel-connected wrapper and a stateless
layout composable taking state + callback lambdas; `@Preview`s target the
stateless one. Mock data lives in `PreviewFixtures` (`core/ui/.../UiPreviews.kt`),
with design-system previews in `core/designsystem/.../ComponentPreviews.kt` and
per-feature previews next to each screen. Follow this split when adding or
reworking a screen so it stays previewable without Hilt/ViewModels.

### Known caveats (from README, still relevant)

- **Navigation 3 is pre-1.0**: `navigation3` and `lifecycle-viewmodel-navigation3`
  versions in `gradle/libs.versions.toml` are alphas and may need bumping; the
  `NavDisplay` / `entryProvider` / decorator API has shifted between alphas
  (notably the `onBack(count)` signature used in `NeonApp.kt`).
- **Downloadable fonts** (Space Grotesk + Manrope): if they silently fall back
  to the system font, re-copy `core/designsystem/src/main/res/values/font_certs.xml`
  from the AndroidX downloadable-fonts docs — the base64 certs must match exactly.
- Streaming is intentionally not implemented yet (parity with the Flutter
  version) — don't treat its absence as a bug. **Push notifications, by contrast,
  are now implemented** (see Push notifications above), ahead of the Flutter
  sibling. (The media viewer is also implemented:
  `core/ui/.../media/MediaPreviewScreen.kt`, opened via
  `Navigator.openMediaPreview`; `MediaGrid` falls back to it when no
  custom click handler is given.)
