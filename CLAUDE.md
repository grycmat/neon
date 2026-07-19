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
- From the CLI: run `gradle wrapper` once (the wrapper `.jar` is not committed), then:
  - `./gradlew :app:assembleDebug` — build the debug APK
  - `./gradlew :app:installDebug` — build and install on a connected device/emulator
  - `./gradlew build` — full build of all modules
  - `./gradlew :core:data:build` (etc.) — build a single module
- No secrets are required to build or run. OAuth app registration happens
  dynamically against whatever instance the user enters at login; the redirect
  `neon://oauth` is intercepted inside an in-app WebView, so no manifest
  intent-filter/scheme is needed. Defaults (redirect URI, scopes, default
  instance) live in `core/data/.../NeonConfig.kt`.
- There is currently no automated test suite in this repo.

## Architecture

### Module graph

```
app                   Auth gate, Navigation 3 wiring, HomeShell (swipeable tabs + TopAppBar + FAB), ShellViewModel
core/model            API entities (Status, Account, Poll, Notification, …)
core/network          ApiClient (OkHttp wrapper bound to instance + token)
core/database          Room cache (list_cache / entity_cache tables)
core/data             Repositories: Auth, Timeline, Status, Notification, Account, Media, Search, Settings
core/designsystem     NeonPalette/NeonTheme/typography, Glass* components, NeonBackground, HtmlText
core/ui               StatusCard, MediaGrid, PollView, QuoteCard, StatusActions, AccountRow, AsyncList,
                      MediaPreviewScreen (full-screen viewer), PreviewFixtures,
                      Navigator + StatusActionService singletons (and the NavKeys)
feature/auth          Login + in-app OAuth WebView
feature/timeline      Home / Local / Federated with segmented pills
feature/explore       Trends + search (also pushed for hashtag taps)
feature/notifications Notifications feed
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
  `predictivePopTransitionSpec` metadata to slide up from the bottom like a
  sheet and back down on pop. Don't add further per-entry transition metadata
  or shared-element/hero animations.
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
- Push notifications and streaming are intentionally not implemented yet
  (parity with the Flutter version, which also lacks them) — don't treat their
  absence as a bug. (The media viewer *is* implemented:
  `core/ui/.../media/MediaPreviewScreen.kt`, opened via
  `Navigator.openMediaPreview`; `MediaGrid` falls back to it when no
  custom click handler is given.)
