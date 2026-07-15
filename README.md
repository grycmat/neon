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
app                   Auth gate, Navigation 3 wiring, HomeShell (tabs + FAB), ShellViewModel
core/model            API entities (Status, Account, Poll, Notification, …)
core/network          ApiClient (OkHttp wrapper bound to instance + token)
core/database         Room cache (list_cache / entity_cache)
core/data             Repositories: Auth, Timeline, Status, Notification, Account, Media, Search, Settings
core/designsystem     NeonPalette/NeonTheme/typography, Glass* components, NeonBackground, HtmlText
core/ui               StatusCard, MediaGrid, PollView, QuoteCard, StatusActions, AccountRow, AsyncList,
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

Architecture mirrors the Flutter app: singleton repositories hold
`StateFlow<AsyncState<…>>` per list; `StatusRepository` broadcasts
updated/created statuses and poll updates on `SharedFlow`s, and every
list-holding repository/ViewModel patches its copies — interactions stay in
sync across timelines, thread, notifications and profiles.

## Building

1. Open the `android/` folder in Android Studio (Narwhal or newer) and let it
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
- Media viewer, push notifications and streaming are not implemented (parity
  with the Flutter version, which also lacks them).
