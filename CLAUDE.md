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
                      NeonNavigator + StatusActionHandler CompositionLocals
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
goes through `NeonNavigator` (below), not direct module deps.

### State pattern: singleton repositories + StateFlow

Every list-backed screen is driven by a `@Singleton` repository (in
`core/data`) exposing `StateFlow<AsyncState<T>>` (`AsyncState` in
`AsyncState.kt`: `Idle / Loading / LoadingMore / Refreshing / Ready / Error`
phases plus `hasMore` for pagination). ViewModels collect this state directly
rather than owning their own copies — the repository is the source of truth,
not the ViewModel.

### Cross-screen sync via SharedFlow broadcasts

`StatusRepository` is the hub for all status interactions (favourite, boost,
vote, create, delete). It broadcasts on three `SharedFlow`s:
- `updates` — a status changed (favourite/boost toggle)
- `created` — a new status was posted (reply/quote/compose)
- `pollUpdates` — a poll's tallies changed after voting

Every other list-holding repository (timelines, notifications, profile lists,
thread) subscribes to these and patches its own cached list in place using
`patchStatusList` / `patchPollList` (`StatusListPatch.kt`), which also follow
into boosted/reblogged statuses. This is how a favourite/boost/vote made in
one screen (e.g. a thread) shows up immediately in another (e.g. the home
timeline) without a refetch. When adding a new mutation or a new list screen,
wire it into this broadcast pattern rather than inventing a separate
refresh/callback mechanism.

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
- Routes are serializable `NavKey`s (`app/.../navigation/NavKeys.kt`), pushed
  onto a `NavBackStack` via `entryProvider { entry<SomeKey> { ... } }`.
- `NeonApp` first gates on `ShellViewModel.authStatus` (Unknown / Unauthenticated
  / Authenticated) before mounting the real nav graph.
- Screens never touch the back stack directly. They call methods on
  `NeonNavigator` (a `CompositionLocal` from `core/ui`, implemented by
  `BackStackNavigator` in `NeonApp.kt`), and status-level actions
  (favourite/boost/vote/share/open-mention) go through `StatusActionHandler`,
  another CompositionLocal, so `core/ui` components (`StatusCard`, etc.) stay
  decoupled from `ShellViewModel`/navigation and are reusable across features.
- `HomeShell` hosts the four root tabs (Home / Explore / Notifications /
  Profile) in a `HorizontalPager` with `beyondViewportPageCount = 3` so tab
  state survives swiping, and draws the shared glassmorphic top app bar itself
  — tab screens must not add their own headers or `statusBarsPadding`
  (`ProfileScreen` pads conditionally because it is also pushed standalone).

### Motion & shared elements

- `NeonMotion` (`core/designsystem/.../theme/NeonMotion.kt`) is the single
  motion vocabulary: `screen()` / `quick()` tweens on the M3 emphasized curve,
  `bouncy()` / `settle()` springs for touch feedback. Use these specs for new
  animations instead of ad-hoc `tween`/`spring` values.
- Shared-element ("hero") transitions: `NeonApp` wraps the nav graph in a
  `SharedTransitionLayout` and provides `LocalSharedTransitionScope` +
  `LocalNavAnimatedVisibilityScope` (`core/designsystem/.../SharedElements.kt`).
  Mark hero views with `Modifier.neonSharedElement(key)` — it degrades to a
  no-op when either scope is absent (previews, HomeShell tabs), so it is safe
  in reusable components.

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
  `NeonNavigator.openMediaPreview`; `MediaGrid` falls back to it when no
  custom click handler is given.)
