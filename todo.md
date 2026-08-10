# Neon — Feedback backlog

Tasks collected from user feedback. Each has a description, current-state
notes (checked against the codebase), and a rough implementation plan. Check
the referenced files again before starting — this snapshot is from 2026-08-08.

---

## 1. Multi-account support

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

## 2. Translations (Polish first) + locale-aware default language

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

## 3. Fix background gradient blending

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

## 4. Fix Follow button width on narrow screens

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

## 5. UnifiedPush support for non-Google builds

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

## 6. "Suggest a review" prompt

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

## 7. Status tap opens thread from the top instead of scrolling to the tapped status

Reported: tapping a status in a timeline/list always opens the thread
scrolled to its beginning (ancestors), not to the specific status that was
tapped. In any longer discussion this means scrolling to the bottom of the
list every time.

Current state: `Navigator.openThread(statusId)`
(`core/ui/.../Navigator.kt:122-125`) pushes `ThreadKey(statusId)` with only
the target status id, carrying no "scroll to me" signal. `ThreadScreen.kt`'s
`PhoneThread` (:108-116), `TwoPaneThread` (:138-144), and `EmbeddedThread`
(:210-217) each build a plain `LazyColumn` with an unremembered default
`LazyListState`. `focusItems` (:239-265) always emits ancestors first, then
the focused `FocusedStatus`, and nothing in the file calls
`scrollToItem`/`animateScrollToItem` to jump to the focused entry.

Steps:
- Give each of the three thread layouts (`PhoneThread`, `TwoPaneThread`,
  `EmbeddedThread`) a `rememberLazyListState()` and, once `focusItems`
  resolves the index of the focused status, call
  `listState.scrollToItem(focusedIndex)` (or `animateScrollToItem`) on first
  composition/load, similar to how a "scroll to top" is already done
  elsewhere (`Navigator.scrollToTopHandler`).
- Skip the scroll when there are no ancestors (focused status is already
  first) to avoid an unnecessary animation.

---

## 8. Alt text is never shown to sighted users

Reported: no way to see alt text on photos in a status.

Current state: `MediaAttachment.altText` is only ever passed as Coil's
`contentDescription` (`MediaGrid.kt:158` in `Tile`, and
`MediaPreviewScreen.kt:220`), which is screen-reader-only. There's no visible
"ALT" badge/chip or tap-to-reveal overlay in either file.

Steps:
- In `MediaGrid.kt`'s `Tile` and in `MediaPreviewScreen.kt`, when
  `attachment.altText` is non-blank, render a small "ALT" badge (similar
  treatment to the existing sensitive-content overlay in
  `MediaGrid.kt:96-126`) that on tap shows the full alt text (e.g. a bottom
  sheet/dialog), mirroring how Mastodon's own official apps expose it.

---

## 9. Poll option limit ignores the instance's configured maximum

Reported: the server-side poll option limit isn't respected (e.g. the
reporter's instance allows 6, but Neon caps at 4).

Current state: `ComposeWidgets.kt:311` hardcodes
`if (poll.options.size < 4)` to gate showing "Add option", a plain literal,
not read from anywhere. `PollDraftState` (`ComposeViewModel.kt:36-40`) has no
max-options field, and `core/model/.../Instance.kt` has no poll-limits fields
at all; `ComposeViewModel.init` only fetches `maxStatusCharacters` (:86),
never `configuration.polls.max_options` from `/api/v1/instance`.

Steps:
- Add `maxOptions` (and while at it, `minExpiration`/`maxExpiration` if
  useful) to the `Instance` model's poll configuration, parsed from
  `configuration.polls.max_options`.
- Fetch it alongside `maxStatusCharacters` in `ComposeViewModel.init` and
  thread it into `PollDraftState`.
- Replace the hardcoded `4` in `ComposeWidgets.kt:311` with the fetched
  limit, falling back to Mastodon's documented default (4) if the instance
  doesn't report one.

---

## 10. Status header: name/handle truncate illegibly on one line, and the timestamp shifts position

Reported: display name and handle are packed onto a single line and both get
cut off so short that neither is readable; separately, the "time ago" text
doesn't stay in a fixed spot.

Current state: `StatusCard.kt:142-186` lays out display name, handle, and
timestamp all in one `Row`: `EmojiText` display name (:151-159, `maxLines=1`,
`weight(1f, fill=false)`), handle `Text` (:161-168, same weighting), a
flexible `Spacer(Modifier.weight(1f))` (:169), then the relative timestamp
(:170-174) and optional "edited" suffix (:175-185). Because name and handle
share weighted space with `fill=false`, their rendered width, and therefore
where the timestamp starts, depends on how long each is, so the timestamp's
horizontal position shifts row to row. `ThreadScreen.kt:396-410`'s
`FocusedStatus` already uses a two-line `Column` for name/handle (without a
timestamp in that row), showing the alternate layout exists elsewhere in the
codebase.

Steps:
- Restructure `StatusCard.kt`'s header (:142-186) into two lines: display
  name on the first line, handle on the second (mirroring `FocusedStatus`'s
  `Column` approach), with the timestamp pinned to a fixed position (e.g.
  top-right of the first line) so it no longer moves based on name/handle
  length.
- Check `NotificationRow` (`feature/notifications/.../NotificationsScreen.kt`)
  for the same single-line truncation, since the user's screenshot for this
  was from the notifications list.

---

## 11. Media upload error handling: verify against report of silent failure

Reported: attaching a photo/video that exceeds the server's size limit shows
no error; the "uploading" indicator just disappears and the item vanishes
from the list.

Current state: this doesn't match what the code does today.
`ComposeViewModel.kt:230-247` (`pickMedia`) wraps each upload in
`try`/`catch`, and on failure emits `_errors.tryEmit("Upload failed:
${e.message}")` (:242), always resetting `uploading` in `finally` (:244);
`ApiClient.kt:113-145` throws `ApiException` with the server's
`error`/`error_description` on non-2xx (e.g. 413/422); `ComposeScreen.kt:122`
shows `errors` as a Snackbar. A rejected file is never added to `state.media`
in the first place (only added after a successful upload, :238-239), so it
wouldn't visually "appear then disappear", it just never appears, with a
Snackbar explaining why.

Steps:
- Reproduce on-device against the specific instance/limit mentioned before
  changing anything, the error path already looks wired, so the report may
  reflect an older build, a Snackbar that's easy to miss, or a specific
  server response shape that isn't being classified as an error.
- One real gap worth fixing regardless: in `pickMedia`'s
  `uris.take(...).forEach` (:236), if one file in a multi-select batch
  throws, the `forEach` aborts and any remaining files in that batch are
  silently never attempted (only the failing file's error is shown).
  Consider catching per-file instead of aborting the whole batch, so one
  oversized file doesn't also drop the others silently.

---

## 12. Notification dismiss ("X") button doesn't visibly remove the row

Reported: notifications have an X on the right, seemingly for dismissal, but
it doesn't appear to do anything.

Current state: it is wired to a real call, not a no-op:
`NotificationsScreen.kt:108-113` passes
`onDismiss = { viewModel.dismiss(notification.id) }`, the `IconButton`/
`Icons.Rounded.Close` at :276-288 invokes it, `NotificationsViewModel.kt:78-82`
calls `NotificationRepository.dismiss(id)`
(`core/data/.../NotificationRepository.kt:126`), which hits the real API.
However, `dismiss()` doesn't appear to optimistically remove the item from
the in-memory list or trigger a refetch after success, which would explain
the "nothing happens" perception even though the server-side call succeeds.

Steps:
- After a successful `NotificationRepository.dismiss(id)`, patch the
  cached/in-memory notification list to remove that entry immediately
  (optimistic update), following the same direct-patch pattern used
  elsewhere (`patchStatusList`/`StatusListPatch.kt`) rather than waiting for
  a future refresh.

---

