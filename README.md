# Neon — Native Android Mastodon Client

Kotlin + Jetpack Compose port of the Flutter app (`../flutter`), matching the
glassy pink→purple→cyan design (`Neon Mastodon Client.html`).

## Stack

- **Kotlin 2.2**, JVM 17, AGP 8.11, compileSdk 36 / minSdk 26
- **Jetpack Compose** (Material 3, BOM), downloadable Google Fonts (Space Grotesk + Manrope)
- **Navigation 3** (`androidx.navigation3`) — serializable `NavKey`s, `NavDisplay`, ViewModel-scoped entries
- **Hilt** for DI (`@HiltViewModel` per screen, `@Singleton` repositories)
- **Room** for the offline cache (same schema idea as the Flutter sqflite cache: raw entity JSON keyed by list + position → cache-first rendering)
- **OkHttp + kotlinx.serialization** for the Mastodon REST API (dynamic instance host, so no Retrofit)
- **Coil 3** for images, **DataStore** for credentials/settings

## Modules

```
app                   Auth gate, Navigation 3 wiring, HomeShell (swipeable tabs + TopAppBar + FAB), ShellViewModel
core/model            API entities (Status, Account, Poll, Notification, …)
core/network          ApiClient (OkHttp wrapper bound to instance + token)
core/database         Room cache (list_cache / entity_cache)
core/data             Repositories: Auth, Timeline, Status, Notification, Account, Media, Search, Settings
core/designsystem     NeonPalette/NeonTheme/typography, Glass* components, NeonBackground, HtmlText
core/ui               StatusCard, MediaGrid, PollView, QuoteCard, StatusActions, AccountRow, AsyncList,
                      MediaPreviewScreen (interactive full-screen viewer), Navigator + StatusActionService singletons (and the NavKeys)
feature/auth          Login + in-app OAuth WebView
feature/timeline      Home / Local / Federated with segmented pills
feature/explore       Trends + search (also pushed for hashtag taps)
feature/notifications Notifications feed
feature/thread        Thread view (ancestors → focused → replies)
feature/composer      Composer: media + alt text, polls, CW, visibility, @-autocomplete
feature/profile       Profile, follow lists, edit profile
feature/settings      Theme mode + logout
```

Architecture mirrors the Flutter app: singleton repositories hold
`StateFlow<AsyncState<…>>` per list; after every mutation `StatusRepository`
directly calls the timeline/notification repositories and notifies registered
listener ViewModels (thread, profile), each patching its copies — interactions
stay in sync across timelines, thread, notifications and profiles.

Navigation is a plain singleton: `Navigator` in `core/ui` holds the Nav3 back
stack (bound by `NeonApp` while the shell is on screen) and screens call it
directly; `StatusActionService` does the same for favourite/boost/vote/share.
Screen transitions slide right-to-left on push and mirror back left-to-right
on pop, with the predictive back gesture driving the same slide. The composer
(`ComposeKey`) overrides this to slide up from the bottom like a sheet and back
down on pop. The predictive back gesture is enabled via `android:enableOnBackInvokedCallback="true"`
in the manifest.

Root shell tabs (Home, Explore, Notifications, Profile) are hosted within a `HorizontalPager` to support swipe navigation, keeping their states alive across page swiping via `beyondViewportPageCount = 3`. A shared, glassmorphic `TopAppBar` displays page context and triggers settings.

## Previews & Stateless Screens

Screens are split into a stateful ViewModel-connected wrapper and a stateless layout composable taking state and callback lambdas. Android Studio `@Preview`s target the stateless layout composable. Mock data for previews resides in `PreviewFixtures` (`core/ui/.../UiPreviews.kt`), with common design-system previews in `core/designsystem/.../ComponentPreviews.kt`.

## Layout & Shell Padding

To support the glassy translucent design, root shell screens (like timelines) render under translucent/glassmorphic bars (top app bar and bottom TabBar) using `LocalShellPadding.current` for inset handling. Headers (like the segmented pills on `TimelineScreen` or search bar on `ExploreScreen`) use a translucent solid background and adjust padding dynamically depending on whether they are root tabs or pushed onto the backstack.

## Building

1. Open the repository root folder in Android Studio (Narwhal or newer) and let it
   sync. If you build from the CLI, run `gradle wrapper` once (the wrapper
   `.jar` is not committed) and then `./gradlew :app:assembleDebug`.
2. No secrets needed — OAuth app registration happens dynamically against the
   instance you enter at login (redirect `neon://oauth` is intercepted inside
   the WebView, so no manifest scheme is required). Defaults live in
   `core/data/.../NeonConfig.kt`.

## Known caveats

- **Navigation 3 is pre-1.0**: `navigation3 = 1.0.0-alpha05` and
  `lifecycle-viewmodel-navigation3 = 1.0.0-alpha03` in
  `gradle/libs.versions.toml` may need bumping to the current release; the
  `NavDisplay`/`entryProvider`/decorator API has shifted slightly between
  alphas (notably the `onBack(count)` signature in `NeonApp.kt`).
- **Downloadable fonts**: if Space Grotesk/Manrope silently fall back to the
  system font, re-copy `core/designsystem/src/main/res/values/font_certs.xml`
  from the AndroidX downloadable-fonts docs — the base64 certs must match
  exactly.
- Push notifications and streaming are not implemented (parity with the Flutter version, which also lacks them).

