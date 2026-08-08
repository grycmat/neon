# Neon — Feedback backlog

Tasks collected from user feedback. Each has a description, current-state
notes (checked against the codebase), and a rough implementation plan. Check
the referenced files again before starting — this snapshot is from 2026-08-08.

---

## 1. Multi-account support

Let a user log into more than one Mastodon account and switch between them
without re-authenticating each time.

Current state: `AuthRepository` (`core/data/AuthRepository.kt`) stores exactly
one session in a single `preferencesDataStore(name = "neon_credentials")`, and
`ApiClient` is a singleton bound to one instance host + token. This is a
single-account design end to end, not a partial feature.

Steps:
- Redesign the credential store to hold a list of sessions (instance host +
  token + account id/handle/avatar) keyed by account id, plus an "active
  account" pointer, instead of one flat record.
- `ApiClient` is currently a `@Singleton` bound at runtime — decide whether it
  gets re-bindable (swap host/token on account switch) or whether it needs to
  become per-account. Re-bindable is closer to the existing "thin OkHttp
  wrapper" design and avoids touching every repository constructor.
  `ApiClient` is used by every repository — audit whether any repository
  caches account-specific state in memory (e.g. `StateFlow`s in
  `TimelineRepository`, `NotificationRepository`, etc.) that must be cleared
  or namespaced on switch, since those are app-wide singletons today.
- `CacheStore` / Room cache (`core/database`) keys rows by `(listKey,
  position)` / entity key with no account dimension — needs an account id
  added to cache keys, or a full cache clear on switch (simpler, but loses
  offline data for the inactive account).
  `PushKeyManager`/`PushRepository` (push subscription) and
  `NotificationWidgetBridge`/widget cache are also single-account today and
  need a decision: multi-account push, or push only for the active account.
- Add an account-switcher UI (likely off the Profile tab or a drawer/sheet),
  and a "add account" flow that reuses the existing OAuth WebView login
  (`feature/auth`) without logging out the current session.
- This is the biggest task on this list — consider scoping a first version to
  "switch between accounts, no simultaneous push/streaming for inactive ones"
  before attempting full parity.

---

## 2. Preview of uploaded photos in the composer

Description as filed: show a thumbnail preview of attached photos when
composing a new toot.

Current state: **this already exists.** `MediaStrip` in
`feature/composer/ComposeWidgets.kt:62` renders a 92dp thumbnail
(`AsyncImage` from the attachment's `preview` URL) per attached item, with a
play-icon overlay for video and a remove (X) button, and it's wired into
`ComposeScreen.kt:234`. Re-verify against current behavior in-app before
treating this as still open — if there's a specific case that doesn't show a
preview (e.g. right after picking, before upload finishes), narrow the task
to that case instead of rebuilding the strip.

---

## 3. Swipe between media items in the full-screen preview — DONE

Instead of opening → closing → opening the next photo/video individually in a
timeline post or thread, support swiping left/right between the items of the
same status while already in full-screen preview.

Current state: `Navigator.openMediaPreview(url, previewUrl, type)`
(`core/ui/Navigator.kt:211`) pushes a single-item `MediaPreviewKey(url,
previewUrl, type)`, and `MediaPreviewScreen` (`core/ui/media/
MediaPreviewScreen.kt`) renders exactly one media item with pinch-zoom/pan and
swipe-down-to-dismiss — there's no concept of a sibling list or index.

Steps:
- Extend `MediaPreviewKey` to carry the full list of attachments for the
  status plus a starting index, instead of a single url/previewUrl/type.
- Update `Navigator.openMediaPreview` call sites (`MediaGrid` in `core/ui`,
  any other fallback callers) to pass the full attachment list + tapped
  index.
- Wrap `MediaPreviewScreen`'s current content in a horizontal pager
  (`HorizontalPager`), one page per attachment, keeping the existing
  pinch/zoom/pan/dismiss gesture logic per-page. Watch for gesture conflicts
  between the pager's horizontal swipe and the existing pan/zoom
  `pointerInput` handling — zoomed-in pages likely need to disable pager
  swipe (same pattern most photo viewers use: only swipe pages at scale ==
  1f).
- Confirm swipe-down-to-dismiss still works per-page and doesn't fight the
  pager's own gesture detection.

---

## 4. Alt text for media

Description as filed: let the user add an alt/description message to
attached media.

Current state: **this already exists.** `MediaStrip` shows an "ALT" chip per
attachment (cyan when filled, dim when empty) that opens `AltTextSheet`
(`ComposeWidgets.kt:166`), a bottom sheet with a text field for "Describe this
image", wired through `onEditAlt` back to `ComposeViewModel`. Re-verify in-app
before treating this as open — if the gap is elsewhere (e.g. editing alt text
isn't possible when *editing* an existing post, only when composing new),
narrow the task to that specific gap.

---

## 5. Translations (Polish first) + locale-aware default language

Add Polish as a translated UI language, and default the app's language to
match system language (Polish if the device is set to Polish, English
otherwise) rather than a hardcoded default.

Current state: no localization scaffolding exists yet (English strings are
inline in composables, not extracted to `strings.xml`/resource-qualified
string resources).

Steps:
- Extract user-facing strings across `app` and all `feature/*` modules into
  `res/values/strings.xml` (`stringResource(R.string.x)`), since Compose text
  today is written as literal strings inline. This is the bulk of the work
  and touches nearly every screen — consider doing it module by module.
- Add `res/values-pl/strings.xml` with Polish translations once the base
  strings resource exists.
- Android resource resolution already picks `values-pl` automatically when
  the system locale is Polish, and falls back to `values` (English) for
  everything else — so "default to English, or Polish if system language is
  Polish" mostly falls out of standard resource qualifiers for free, with no
  extra picker needed for the two-language case described. Only add an
  explicit in-app language override setting if the user wants to force a
  language independent of system locale.

---

## 6. Fix background gradient blending

The three blurred accent orbs in `NeonBackground` don't blend smoothly into
the base background color — visible seams/edges instead of a smooth fade.

Current state: `NeonBackground` (`core/designsystem/component/
NeonBackground.kt`) draws `palette.bg` as a flat background, then layers three
`Orb`s, each a `Brush.radialGradient` from `color.copy(alpha = opacity)` to
`color.copy(alpha = 0f)`, blurred by `Modifier.blur(48.dp)`. Two likely causes
worth checking:
- A radial gradient that goes to `alpha = 0f` can still show a visible edge
  where the gradient stop meets the blur radius, especially combined with
  `blur()`'s own edge-clamping behavior — try extending the gradient radius
  past the drawn circle bounds, or adding an intermediate color stop instead
  of a hard two-stop fade.
- `Modifier.blur` is a no-op below API 31 (noted in the file's own comment);
  if the reported issue is seen on an API < 31 device/emulator, the "gradient
  not blending" symptom may actually be "no blur at all," which needs a
  different fix (e.g. a software blur fallback, or accepting a softer
  gradient-only look pre-31).
- Confirm which case (API level, or the gradient stops themselves) reproduces
  the issue before changing anything, since the fix differs.

---

## 7. Fix Follow button width on narrow screens

In `ProfileScreen`, the Follow / Following / Requested button breaks its
inner text on narrow devices instead of fitting on one line.

Current state: `ProfileScreen.kt:376-391` fixes the button to
`Modifier.width(118.dp)` for all three label states ("Follow", "Following",
"Requested"), sitting in a `Row` after three `GlassIconButton`s (mute, block,
report) with 8dp spacers between each. "Requested" is the longest label and
is the one most likely to wrap at 118dp, especially at larger font-scale
settings.

Steps:
- Reproduce at a narrow width (small phone, or a large system font-scale
  accessibility setting) to confirm it's a hard-wrap of "Requested"/"Following"
  rather than the whole row overflowing the screen.
- If it's just the button too narrow for its longest label: remove the fixed
  `width(118.dp)` and let the button size to its content (`widthIn(min = ...)`
  instead of a hard `width`), or measure the longest label ("Requested") and
  set that as the fixed width instead of 118dp.
- If the whole row (3 icon buttons + follow button) doesn't fit on very
  narrow screens: consider wrapping the row (`FlowRow`) or moving the
  secondary actions (mute/block/report) behind an overflow menu on narrow
  widths, similar to how other screens in this codebase adapt at the
  `isBigScreen()` breakpoint (`core/ui/BigScreen.kt`) — though this is a
  narrow-screen problem, not a big-screen one, so it needs its own width
  check rather than reusing that helper.

---

## 8. Background image support

Let the user set a custom background image behind the glassy UI, instead of
only the flat `palette.bg` + accent orbs.

Current state: `NeonBackground` always draws `palette.bg` as a flat
`Modifier.background(...)`, with no image layer. No settings entry exists for
this yet (`SettingsRepository` today only has things like `dynamicColorEnabled`
— check that file for the current shape before adding to it).

Steps:
- Decide the source: a bundled set of preset background images, or a
  user-picked photo (`ActivityResultContracts.PickVisualMedia`, same
  mechanism the composer's media picker likely already uses — check
  `feature/composer` for the existing picker call before adding a second
  one).
- Persist the choice (preset id, or a copied content-uri) via
  `SettingsRepository`/DataStore, same pattern as `dynamicColorEnabled`.
- In `NeonBackground`, replace/augment the flat `background(palette.bg)` with
  an `Image`/`AsyncImage` layer when a background image is set, keeping the
  glass surfaces' blur/translucency working on top of it — glass components
  in `core/designsystem` rely on there being *something* visible behind them
  to blur, so verify `Glass*` components still read correctly over a busy
  photo (may need a dimming/scrim layer for contrast, especially for light
  theme).
- Respect the existing dark/light theme split (`NeonPalette.Dark`/`.Light`) —
  decide whether a custom background image is theme-independent or swaps per
  theme.

---

## 9. UnifiedPush support for non-Google builds

Support push notifications without Google/FCM, for users on de-Googled
devices or F-Droid-style builds, via UnifiedPush.

Current state: push today is FCM-only end to end — `core/data/push/
PushKeyManager` (keys), `PushRepository` (subscribes via the self-hosted
`mastodon-fcm-relay`, endpoint keyed by FCM token), and the two receive paths
`feature/notifications/NeonFirebaseMessagingService` +
`NeonC2dmReceiver` both assume Firebase. No UnifiedPush dependency exists in
the repo yet.

This needs a design discussion before implementation — flagged as such in the
original feedback ("we need to discuss how to approach two application
versions"). Open questions to resolve first:
- Single build with both FCM and UnifiedPush code paths (runtime choice) vs.
  two separate build flavors/variants (one Play-store FCM build, one
  UnifiedPush-only build for F-Droid/other stores) — affects Gradle module
  structure (`build.gradle.kts` flavor dimensions) and CI.
- Whether `WebPushDecryptor` (already relay/transport-agnostic, pure crypto
  over the RFC 8291 payload) can be reused as-is for the UnifiedPush path —
  likely yes, since it doesn't know about FCM, only about the Web Push
  payload format. The FCM-specific pieces to replace are `PushKeyManager`'s
  registration target and the receive-side (`NeonFirebaseMessagingService`
  equivalent becomes a UnifiedPush `MessagingReceiver`).
- Whether the self-hosted `mastodon-fcm-relay` is still needed for the
  UnifiedPush path, or whether UnifiedPush distributors talk to Mastodon's
  Web Push endpoint more directly.

---

## 10. Full-screen preview of profile photo (avatar)

Tapping a profile's avatar should open it full-screen, same as tapping status
media does today.

Current state: `MediaPreviewScreen` + `Navigator.openMediaPreview` already
provide a generic full-screen image viewer (pinch-zoom, pan,
swipe-down-to-dismiss) — it just isn't wired up from the avatar in
`ProfileScreen`.

Steps:
- In `ProfileScreen.kt`, find the avatar composable (`NeonAvatar` usage near
  the header) and add a tap handler that calls
  `Navigator.openMediaPreview(account.avatar, type = "image")` (check the
  `Account` model in `core/model` for the exact full-resolution avatar field
  name vs. the thumbnail one — Mastodon accounts expose both `avatar` and
  `avatar_static`).
- This is a small, self-contained task — no new screen needed, just reusing
  the existing preview infra, similar to how status media already opens it.

---

## 11. "Suggest a review" prompt

Periodically prompt happy/engaged users to leave a Play Store review.

Current state: no in-app review integration exists yet. `app/update/
AppUpdateController.kt` is a close structural analog worth mirroring — a
Hilt `@Singleton` wrapping a Play-services Task API, gated on install source
(`installingPackageName == "com.android.vending"`), called from a
`MainActivity` `LaunchedEffect`.

Steps:
- Add the Play In-App Review library
  (`com.google.android.play:review-ktx`), `:app`-module only (same reasoning
  as `app-update-ktx`: no `core/*`/`feature/*` module needs it).
- Build an `InAppReviewController` following `AppUpdateController`'s shape:
  wraps `ReviewManagerFactory`'s `requestReviewFlow()`/`launchReviewFlow()`
  Tasks, gated on Play install source the same way, wrapped since it throws
  off-Play.
- Decide the trigger heuristic (e.g. after N successful sessions, or after a
  positive-signal action) and persist a "don't ask again for X" / "already
  reviewed" flag via `SettingsRepository`/DataStore, same nagging-guard
  pattern `AppUpdateController` uses (`dismissedUpdateVersion`) — the Play
  review API itself doesn't tell the app whether the dialog was actually
  shown or the user rated, so the app-side guard is the only rate-limiting
  available.

---

## 12. Remove "Live" label from the Notifications tab app bar

Small cleanup: drop the "Live" pill shown next to the "Notifications" title
in the shared top app bar.

Current state: `app/HomeShell.kt:583-586`:
```kotlin
if (p == 1) {
    Spacer(Modifier.width(8.dp))
    NeonLabel("Live")
}
```
Delete this `if` block (both the `Spacer` and `NeonLabel`). Check whether
`NeonLabel` is still used elsewhere in `HomeShell.kt` before removing its
import (it's also used a few lines below for the instance-host pill on the
Home tab, so the import stays).

---

## 13. Rename "post" to "toot" in UI copy

Use Mastodon's original "toot" terminology instead of "post" throughout
user-facing strings (compose button, FAB, action labels, etc.), matching the
Flutter sibling app's tone if it already does this.

Steps:
- Grep the app for user-facing occurrences of "Post"/"post" as a noun/verb
  referring to a status (compose button label, FAB content description,
  success toasts, etc.) — careful not to rename unrelated uses (e.g. HTTP
  POST, "posted X ago" timestamps might read fine either way, Kotlin
  `postValue`-style code isn't UI copy).
- If tackled after task 5 (i18n string extraction), this becomes a
  straightforward string-resource edit; if done first, it's inline string
  edits across composables that will need to be re-touched during extraction
  anyway — doing 5 first avoids doing this copy pass twice.
