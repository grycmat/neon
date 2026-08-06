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
core/data             Repositories: Auth, Timeline, Status, Notification, Account, Bookmark, Conversation, Media,
                      Search, Settings, List, Filter, Tag (followed hashtags);
                      push/ (Web Push subscription + on-device decryption, see Push notifications)
core/designsystem     NeonPalette/NeonTheme/typography, Glass* components, NeonBackground, HtmlText
core/ui               StatusCard, MediaGrid, PollView, QuoteCard, LinkPreviewCard, StatusActions, AccountRow, AsyncList,
                      VideoPlayer (ExoPlayer), MediaPreviewScreen (full-screen viewer), EditHistorySheet, PreviewFixtures,
                      Navigator + StatusActionService singletons (and the NavKeys)
feature/auth          Login + in-app OAuth WebView
feature/timeline      Home / Local / Federated with segmented pills, plus hashtag and list timelines
feature/explore       Trends (with TrendSpark sparklines) + search (also pushed for hashtag taps)
feature/notifications Notifications feed + filtered-notification requests queue + follow-request review;
                      NeonFirebaseMessagingService + NeonC2dmReceiver + PushMessageHandler + FcmTokenProvider (push)
feature/messages      Direct messages: Conversation list + new-message composer (Mastodon has no
                      separate DM system — a Conversation just groups visibility="direct" statuses)
feature/thread        Thread view (ancestors → focused → replies)
feature/composer      Composer: media + alt text, polls, CW, visibility, @-autocomplete
feature/profile       Profile, follow lists, bookmarks, edit profile (incl. field editor), list membership
feature/settings      Theme mode + Material You toggle + logout, keyword filters, list management, followed-hashtag management
feature/widget        Home-screen notifications widget (Jetpack Glance) — see Home-screen widget below
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
  `prependCreated`, `NotificationRepository.applyStatusUpdate`,
  `BookmarkRepository.applyStatusUpdate`, and `ConversationRepository
  .applyStatusUpdate` / `applyStatusDelete` (all injected singletons) — a new
  `visibility="direct"` status instead calls
  `ConversationRepository.onDirectStatusCreated()`, which just refetches since
  the create endpoint returns a bare `Status`, not a `Conversation`, and
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
Reading the response body is forced onto `Dispatchers.IO` even though it runs
after the call's `await()` continuation already resumed: that resumption lands
on whatever dispatcher the caller used (often `viewModelScope`'s Main), and a
chunked response (observed from e.g. `social.vivaldi.net`) isn't fully
buffered yet, so `body.string()` can still block on the socket — which trips
`NetworkOnMainThreadException` and silently breaks login/requests without
that `withContext`.

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

### Streaming

`StreamingRepository` (`core/data`) holds a live Mastodon `user` WebSocket (new/edited/deleted
statuses + notifications, multiplexed on one connection) via `StreamingClient` (`core/network`).
It only runs while foregrounded and authenticated — `combine(AuthRepository.status,
foreground)`, with `foreground` driven by `ShellViewModel.setForeground` from `MainActivity`'s
lifecycle — and reconnects with exponential backoff (2s–30s) on drop or failure. Backgrounded
delivery stays on the FCM push path above; the two don't overlap. Events patch the same
`TimelineRepository` / `NotificationRepository` singletons the REST-driven mutations do, so a
status arriving over the socket flows through the same `applyStatusUpdate` /
`applyStatusDelete` / `prependCreated` / `prependNotification` calls described in Cross-screen
sync, and reaches the widget via the same `NotificationWidgetBridge.redraw()` path.

### Home-screen widget

`feature/widget` is a **Jetpack Glance** app widget listing the newest notifications. It is the
only place in the app that isn't Compose UI — Glance emits `RemoteViews`, so none of `core/ui`
or `core/designsystem`'s composables can be reused. It borrows the palette (`NeonPalette.Dark` /
`.Light` are plain data, no composition needed) and `htmlToPlainText` / `relativeTime`, and
re-states the layout in Glance primitives.

- **Data** comes from the Room notifications cache via `NotificationRepository.cachedNotifications`,
  never the in-memory `state`: the widget is routinely composed in a process the system started for
  a broadcast, where no ViewModel ever ran a load. `NotificationRepository.refreshForWidget()` is
  the fetch — deliberately **not** `refresh()`, which would move the in-memory phase out of `Idle`
  and make the in-app screen's first `load()` return early on a background fetch's result.
- **`AuthRepository.ensureConfigured()`** points `ApiClient` at the stored session without the
  network round-trip `restore()` does. Any background entry point needs it; `PushMessageHandler`
  now calls it too, which is what makes push notifications' thread deep-link resolve when the app
  wasn't already running.
- **Refresh triggers**, all converging on re-running `provideGlance`:
  - push (backgrounded) — `PushMessageHandler` awaits `NotificationWidgetBridge.refresh()` after
    posting the notification, so the fetch runs inside the receiver's `goAsync` window;
  - streaming (foregrounded) — `NotificationRepository.prependNotification` persists, then redraws;
  - in-app mutations — `refresh` / `dismiss` / `clear` redraw from `persist()`;
  - the refresh button — `RefreshWidgetAction`;
  - cold path — `NotificationWidgetRepository.refreshIfStale()` runs inside `provideGlance`, so the
    system's `updatePeriodMillis` tick and first placement fetch too.
  `NotificationWidgetReceiver` deliberately does **not** override `onUpdate`: Glance's own
  implementation already claims the receiver's single `goAsync()` PendingResult, and skipping
  `super` would lose the `GlanceAppWidgetManager` bookkeeping `updateAll` needs.
- **`NotificationWidgetBridge`** (`core/data`) is how the data layer reaches the widget without
  `core/*` depending on `feature/*` — same plain-singleton pattern as `Navigator` /
  `StatusActionService`, installed by `NeonApplication.onCreate` from `NotificationWidgetHost`.
  `redraw()` is fire-and-forget (callers are on the main thread); `refresh()` suspends so a push
  entry point's PendingResult keeps the process alive for it.
- **Binder budget** shapes two decisions that look arbitrary otherwise: `SizeMode.Single` (Exact /
  Responsive compose one RemoteViews tree *per host size*, duplicating every avatar bitmap), and
  `MAX_ROWS` = 10 with avatars capped at 100px in `WidgetAvatars`. `WidgetAvatars` also composites
  the type badge *into* the avatar bitmap, because a Glance `Box` has one `contentAlignment` for
  all children and so cannot offset a badge over an image.

### In-app updates

Google Play in-app updates (`com.google.android.play:app-update-ktx`), all in `app/.../update/`
— no `core/*` or `feature/*` module needs them, so the Play dependency stays in `:app`.

- `AppUpdateController` is the whole implementation: a Hilt `@Singleton` wrapping
  `AppUpdateManager`, same shape as `FcmTokenProvider` (thin coroutine wrapper over a
  Play-services Task API). It exposes `StateFlow<AppUpdateUiState>` — `Idle` / `Downloading` /
  `ReadyToInstall`, deliberately **free of Play types** so the Compose layer never observes an
  unstable `AppUpdateInfo`.
- **Strategy is priority-driven**: `FLEXIBLE` (background download, app stays usable) for routine
  releases, escalating to Play's blocking `IMMEDIATE` flow only when `updatePriority() >= 4` or
  `clientVersionStalenessDays() >= 14`. Note `updatePriority` is **not** settable in the Play
  Console UI — it's `inAppUpdatePriority` on `Edits.tracks.releases` via the Play Developer API,
  fixed at publish time. Without setting it per-release, only the 14-day staleness rule ever
  triggers the immediate flow.
- **`checkAndStart(launcher)` is the single entry point** (the `syncPushRegistration` pattern),
  called from a `MainActivity` `LaunchedEffect` keyed on the **existing** `isAppForeground` state,
  so it runs on cold start *and* every resume. The resume call is not optional — it's what
  re-enters a stalled immediate update (`DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS`) and what
  surfaces a flexible download that completed while the app was away (`installStatus() ==
  DOWNLOADED`), which is also how the prompt survives process death.
- **Gated on install source**, not `BuildConfig.DEBUG` — `installingPackageName ==
  "com.android.vending"`. Deliberate: it keeps internal-app-sharing builds (the only real way to
  test this) working, and keeps sideloaded/dev builds from logging a failed check every resume.
  `requestAppUpdateInfo()` throws off-Play, so it is always wrapped.
- **Two guards against nagging**: `promptedThisProcess` (in-memory, one flexible offer per
  process however often we re-check) and `SettingsRepository.dismissedUpdateVersion` (persisted
  `availableVersionCode` the user cancelled). Immediate flows set neither — an urgent update is
  meant to re-prompt on every launch.
- `MainActivity` owns the `StartIntentSenderForResult` launcher (the codebase has no
  `onActivityResult` override anywhere — keep it that way) and renders `UpdateReadyDialog`
  inside `NeonTheme` next to `NeonApp`, so its own window floats above the shell and any pushed
  Nav3 screen.

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
  `predictivePopTransitionSpec` metadata to slide up from the bottom of the
  screen on push and slide back down on pop, instead of the horizontal slide.
  Don't add further per-entry transition metadata or shared-element/hero
  animations.
- `NeonApp` first gates on `ShellViewModel.authStatus` (Unknown / Unauthenticated
  / Authenticated) before mounting the real nav graph.
- Navigation and status actions are **plain singleton `object`s in `core/ui`**,
  called directly from any composable (no CompositionLocals, no interfaces):
  - `Navigator` holds `var backStack: NavBackStack?` — `NeonApp` binds it in a
    `DisposableEffect` while the authenticated shell is on screen; while null
    (previews, login) every call no-ops. Screens call `Navigator.openThread(id)`,
    `Navigator.back()`, etc.
  - `StatusActionService` (favourite/boost/vote/share/open-mention/open-url) is
    initialized from `NeonApplication.onCreate` with Hilt-injected repos; it
    owns a Main-dispatcher scope, shows failures as Toasts, and resolves
    mention taps straight to `Navigator.openProfile`.
- `HomeShell` hosts the four root tabs (Home / Explore / Notifications /
  Profile) in a `HorizontalPager` with `beyondViewportPageCount = 3` so tab
  state survives swiping, and draws the shared glassmorphic top app bar itself
  — tab screens must not add their own headers or `statusBarsPadding`
  (`ProfileScreen` pads conditionally because it is also pushed standalone).
  Tapping the bar's title area invokes `Navigator.scrollToTopHandler?.invoke()`
  directly (same file, no callback param threaded through). Only the Home tab
  (`TimelineScreen`) registers it, in a `DisposableEffect` keyed on an
  `isActiveTab` param `HomeShell` passes as `page == pagerState.currentPage` —
  needed because `beyondViewportPageCount` keeps adjacent pages composed, so
  without that guard an off-screen preloaded Home instance could steal the
  handler. The handler body is identical to the "N new toots" pill's
  `onClick` (`animateScrollToItem(0)` + `clearNewToots(kind)`), so tapping the
  bar acts exactly like tapping that pill. Null on every other tab, so the tap
  silently no-ops there — same null-is-a-no-op convention as
  `threadPaneHandler` below.

### Clickable status/bio content

`HtmlText` (`core/designsystem/.../component/HtmlText.kt`) parses Mastodon HTML
(`parseStatusHtml`, `core/designsystem/.../util/Html.kt`) into `Text` / `Mention`
/ `Hashtag` / `Link` segments and renders each as a `LinkAnnotation.Clickable`
span *only* when the matching `on*Click` callback is passed — a null callback
still gets accent/underline styling but renders inert, so every call site must
wire all three or a tap silently no-ops. `HtmlText` also exposes the underlying
`Text`'s `onTextLayout`; `StatusBody` (`StatusCard.kt`) uses it in feed/list
contexts (`truncatable = true`) to clip the body at `FeedBodyMaxLines` (15) and
show a "Show more" hint based on `TextLayoutResult.hasVisualOverflow` — real
visual overflow, not a character-count guess, since emoji/mention/hashtag
inline content and font metrics make line count unpredictable from the raw
string length. The focused status in `ThreadScreen` renders with
`truncatable = false` (the default) so it's never clipped.

The four call sites (`StatusBody` in `StatusCard.kt`, used by feed cards and
`ThreadScreen`'s focused status; `QuoteCard`; `EditHistorySheet`;
`ProfileScreen`'s bio) all wire:
- `onHashtagClick` → `Navigator.openHashtagSearch(tag)`, which pushes
  `HashtagKey("#$tag")` into `ExploreScreen(initialQuery = …)` — Explore's
  search prepopulated and run, the same destination a trending-tag tap lands
  on. This is deliberately a different method/route from `Navigator.openHashtag(tag)`
  (pushes `HashtagTimelineKey` → the dedicated `HashtagTimelineScreen` with its
  follow/unfollow toggle), which stays reserved for explicit tag-chip taps —
  `ProfileScreen`'s featured-tags row and `ManageFollowedHashtagsScreen`.
- `onMentionClick` → `StatusActionService.openMention(status, acctOrUrl)`,
  which cross-references the tapped text/href against that status'
  structured `mentions` list before resolving the match via
  `SearchRepository.searchAccounts` and `Navigator.openProfile` (a live
  network round trip; a miss silently no-ops). `EditHistorySheet` reuses the
  parent status' `mentions` for this, since Mastodon doesn't expose
  per-revision structured mentions. `ProfileScreen`'s bio has no `Status` to
  cross-reference, so it calls the `openMention(acctOrUrl)` overload instead,
  which derives the handle straight from the tapped text/href.
- `onLinkClick` → `StatusActionService.openUrl(url)` — a plain `ACTION_VIEW`
  intent, the same pattern already used inline for the profile "Server info"
  and privacy-policy buttons in `ProfileScreen.kt`.

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

### Theming & Material You

`NeonPalette` (`core/designsystem/.../theme/NeonPalette.kt`) is the single source of every color
in the app: two static instances, `Dark`/`Light`, hand-tuned around the pink→purple→cyan brand
trio (`NeonAccents`). `NeonTheme(darkTheme, dynamicColor, content)` provides the active instance
via `LocalNeonPalette`; `neonColorScheme()` derives the M3 `ColorScheme` fed to `MaterialTheme`
from whichever palette is active, so it never needs separate wiring.

Material You (dynamic color, Android 12+) is opt-in and off by default:
- `SettingsRepository.dynamicColorEnabled` → `ShellViewModel` (read, for `MainActivity`) and
  `SettingsViewModel` (read/write, for the Settings screen) → `MainActivity` passes it into
  `NeonTheme(dynamicColor = ...)`. The Settings screen exposes it as "Match wallpaper colors",
  hidden below API 31.
- When on, `NeonPalette.dynamic(context, isLight)` copies the static `Dark`/`Light` instance —
  glass surfaces, text, borders stay untouched — and re-derives only the accent-driven fields
  (`gradientColors`, `avatarGradients`, `orbColors`, ink/fill/glow colors, and the new
  `accentPink`/`accentPurple`/`accentCyan` (+ `Dim`/`Soft`) fields) from
  `dynamicDarkColorScheme`/`dynamicLightColorScheme`'s primary/tertiary roles: the "voltage" trio
  (from the dark scheme, for gradients/avatars/orbs) and the "ink" trio (from the light scheme, for
  hashtags/labels on light surfaces). There's no third wallpaper-derived hue, so the middle
  "purple" gradient stop is a `lerp` midpoint between pink and cyan.
- Everything that used to reach for `NeonAccents` directly outside `NeonPalette.kt` (the avatar
  fallback gradient in `NeonAvatar.kt`, the CTA glow in `Glass.kt`'s `GradientButton`, both FAB
  glows in `HomeShell.kt`) now reads `palette.accentPink`/`accentPurple` instead, so those follow
  dynamic color too. `NeonAccents` itself is referenced only from `NeonPalette.kt` now.
- Themed launcher icon (monochrome adaptive-icon layer, `app/src/main/res/mipmap-anydpi-v26/`) is
  the other half of Material You and needs no Compose wiring — it's handled entirely by the OS.
- Full design rationale and implementation notes: `material_you.md`.

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
- Both streaming (see Streaming above) and push notifications are implemented, ahead of the
  Flutter sibling — don't treat either as missing. (The media viewer is also implemented:
  `core/ui/.../media/MediaPreviewScreen.kt`, opened via
  `Navigator.openMediaPreview`; `MediaGrid` falls back to it when no
  custom click handler is given.)
