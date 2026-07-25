# Neon — MVP Roadmap

Full codebase audit and step-by-step plan to a shippable release.

---

## Current State Summary

| Area | Status |
|------|--------|
| Auth (OAuth + offline restore) | ✅ Complete |
| Timeline (Home / Local / Federated) | ✅ Complete |
| Explore (trends + full search) | ✅ Complete |
| Notifications feed (all types) | ✅ Complete |
| Thread view (ancestors → focus → replies) | ✅ Complete |
| Composer (text / media / poll / CW / visibility) | ✅ Complete |
| Profile + Edit + Follow lists | ✅ Complete |
| Settings (theme + logout) | ✅ Complete |
| Design system + adaptive big-screen layouts | ✅ Complete |
| Room cache (offline-first, cache-first) | ✅ Complete |
| CW expand / collapse in status card | ✅ Complete |
| Sensitive media blur | ✅ Complete |
| Pull-to-refresh spinner | ✅ Complete |
| Bookmarks | ✅ Complete |
| Status edit / delete-and-redraft | ✅ Complete |
| Mute / Block / Report | ✅ Complete |
| Hashtag timeline | ✅ Complete |
| Push notifications | ✅ Complete (FCM relay + on-device Web Push decryption) |
| Video playback (Media3 / ExoPlayer) | ✅ Complete |
| Streaming (WebSocket) | ❌ Post-MVP |

---

## Gap analysis vs. the official Mastodon Android app

Compared against `mastodon/mastodon-android` (the official client) on GitHub —
its fragment/API-request package layout was audited directly. Neon covers the
core toot lifecycle (read/write/boost/fave/vote/bookmark/moderate) and beats
the official app on push notifications and big-screen support, but is missing
several features that are table-stakes in every mainstream Mastodon client
(official app, Ivory, Ice Cubes, Elk). These are broken out as **Milestone 5**
below, ahead of Release Prep.

Not counted as gaps (deliberately out of scope / already tracked): in-app
account signup & onboarding carousel (Neon only logs into existing accounts,
per `NeonConfig`), read markers (sync-position bookkeeping, invisible to the
user), donation/tipping screens, and anything already in the Post-MVP Backlog
below (streaming, multi-account, etc.).

---

## Milestone 1 — Bug-fix & Stability
> **Estimate: 1–2 days**
> Goal: make what exists work correctly end-to-end.

- [x] **Fix pull-to-refresh spinner** — `PullToRefreshBox(isRefreshing = false, …)` is hardcoded in
  `TimelineScreen`, `NotificationsScreen`, and `ThreadScreen`. Wire the actual
  `AsyncPhase.Refreshing` / `AsyncPhase.LoadingMore` state from each ViewModel.

- [x] **Content Warning expand / collapse** — `Status.spoilerText` is decoded but `StatusCard`
  renders it without a show/hide toggle. Add a collapsible CW banner that hides the body
  until tapped; remember the expanded state per-item.

- [x] **Sensitive media blur** — `Status.sensitive` flag exists in the model. `MediaGrid` should
  blur all thumbnails and show a "Show sensitive content" overlay tap target.

- [x] **Startup error boundary** — `AuthRepository.restore()` can throw (network down on first
  launch). The `AuthStatus.Unknown` spinner loops forever. Catch the exception and
  transition to `Unauthenticated` with a retry button.

- [x] **Status context menu** — add a long-press bottom sheet on `StatusCard` with:
  - *Own toots:* Edit, Delete, Delete & re-draft
  - *Other toots:* Mute account, Block account, Report

---

## Milestone 2 — Core Missing Features
> **Estimate: 3–5 days**
> Goal: close the feature gaps that users expect in any Mastodon client.

- [x] **Bookmarks**
  - Add `bookmark` / `unbookmark` toggle to `StatusRepository`
    (`POST /api/v1/statuses/:id/bookmark|unbookmark`).
  - Add bookmark icon to `StatusActions` (next to share).
  - Add `BookmarkRepository` (`GET /api/v1/bookmarks`, paginated).
  - Add `BookmarksKey` nav key, `BookmarksScreen` (reuse `AsyncList` + `StatusCard`).
  - Wire route in `NeonApp.kt` and add entry point (settings or profile header).

- [x] **Status edit**
  - Add `PUT /api/v1/statuses/:id` in `StatusRepository`.
  - Open `ComposeScreen` pre-filled with the existing text / media / poll / CW when "Edit"
    is picked from the context menu. Post as an edit (not a new status).

- [x] **Delete & re-draft**
  - `DELETE /api/v1/statuses/:id` (already in `StatusRepository.delete()`).
  - Open `ComposeScreen` with the deleted status's text pre-filled.

- [x] **Mute / Block**
  - Add `mute(id)` / `unmute(id)` and `block(id)` / `unblock(id)` to `AccountRepository`
    (`POST /api/v1/accounts/:id/mute|unmute|block|unblock`).
  - Surface in the status context menu and on the `ProfileScreen` action bar.

- [x] **Report**
  - Add `POST /api/v1/reports` (with optional comment) to `AccountRepository`.
  - Surface as the last item in the status context menu.

- [x] **Notification dismiss / clear**
  - Add dismiss icon (×) per `NotificationRow` → `POST /api/v1/notifications/:id/dismiss`.
  - Add "Clear all" action in `NotificationsScreen` top bar →
    `POST /api/v1/notifications/clear`.

---

## Milestone 3 — Media & Timeline Quality
> **Estimate: 2–3 days**
> Goal: media works fully, timeline feels alive.

- [x] **Video / gifv playback**
  - Add `androidx.media3:media3-exoplayer` + `media3-ui` dependencies.
  - Replace the static thumbnail in `MediaGrid` for `type == "video"` / `"gifv"` with an
    inline `ExoPlayer` composable (muted, looping for gifv; unmuted on tap for video).
  - Full-screen video in `MediaPreviewScreen`.

- [x] **Hashtag timeline**
  - Add `HashtagTimelineScreen` that reuses the `TimelineRepository`-style pattern but hits
    `GET /api/v1/timelines/tag/:hashtag`.
  - Change `Navigator.openHashtag()` to push `HashtagTimelineKey` instead of `HashtagKey`
    (which currently opens Explore / search).

- [x] **"New toots" banner + scroll-to-top**
  - After a pull-to-refresh or timed background check, show a pill-shaped banner
    "↑ N new toots" that, when tapped, scrolls the list to the top.
  - Requires tracking the first visible item index in `TimelineScreen`.

- [x] **Link preview card**
  - Add `card` field to the `Status` model
    (maps to `GET /api/v1/statuses/:id` → `card` object: title, description, image, url).
  - Add a `LinkPreviewCard` composable rendered below `StatusBody` when `card != null`.

---

## Milestone 4 — Notifications & Accessibility
> **Estimate: 2–3 days**
> Goal: the app can reach users even when closed; screen-reader users can use it.

- [x] **Push notifications**
  - Registers a Web Push subscription (`POST /api/v1/push/subscription`, RFC 8030/8291,
    `standard=true` aes128gcm) via `PushRepository`, with the on-device P-256 keypair +
    auth secret from `PushKeyManager` (stored in `EncryptedSharedPreferences`).
  - Delivery is over **FCM data messages** relayed through a self-hosted
    `mastodon-fcm-relay` (endpoint path carries the FCM token) — the relay never sees
    plaintext. `NeonFirebaseMessagingService` decrypts on-device (`WebPushDecryptor`,
    aes128gcm + legacy aesgcm) and posts via `NotificationManager`.
  - `ShellViewModel.syncPushRegistration` reconciles subscription state against auth +
    the notifications setting + `POST_NOTIFICATIONS` permission; logout tears it down.
  - Deep-links taps into `ThreadScreen` / `NotificationsScreen` via
    `Navigator.handleNotificationClick`.
  - Requires `google-services.json` + `secrets.properties` (`RELAY_BASE_URL`); see README.

- [x] **Accessibility pass**
  - Added meaningful `contentDescription` to all icon-only buttons
    (`GlassIconButton`, `ComposeFab`, `StatusActions` buttons, `HomeShell` tab
    rail/bar icons, the `EditProfileScreen` avatar badge).
  - Added `role = Role.Button` to every clickable in `Glass.kt`
    (`GlassButton`/`GlassIconButton`/`GradientButton` — covers ~20 call
    sites app-wide), plus `HomeShell`'s FAB/tabs and `ProfileScreen`'s `Stat`.
  - `StatusActions.ActionItem` now exposes one merged `clearAndSetSemantics`
    node per action ("Reply, 12", "Boost"/"Undo boost", "Favourite, 3",
    "Bookmark"/"Remove bookmark", "Share") instead of an unlabeled icon.
  - Verified focus traversal order in `ComposeScreen` and `ProfileScreen`:
    both are plain top-to-bottom `Column`/`Row` layouts with no z-index or
    out-of-order overlays (the `SnackbarHost` in `ComposeScreen` is composed
    last and sits visually last, so no `traversalIndex` override was needed).

- [x] **Error retry button**
  - `AsyncList` already shows error text. Add a "Retry" `GlassButton` below it that calls
    the `onRefresh` callback.

- [x] **Account fields in Edit Profile**
  - `EditProfileViewModel` now tracks `fields: List<AccountField>` in its
    UI state, seeded from `Account.fields` (HTML values run through
    `htmlToPlainText` like the bio) via `EditProfileScreen`'s `start()` call.
  - Added `onFieldNameChange`/`onFieldValueChange`/`addField`/`removeField`,
    capped at `MAX_ACCOUNT_FIELDS = 4`.
  - New `ProfileFieldsEditor` composable in `EditProfileScreen.kt`: a
    Label/Content `GlassField` pair per row with a remove (×) `GlassIconButton`,
    plus a "+ Add field" `GlassButton` shown while under the cap.
  - `save()` pads the list to exactly 4 slots (blank name/value for unused
    ones) before calling `updateCredentials`, since Mastodon's
    `fields_attributes` replaces the whole set — otherwise a removed field
    would survive server-side.

---

## Milestone 5 — Feature Parity Gaps (vs. official app)
> **Estimate: 5–8 days**
> Goal: close the gaps that make Neon feel thinner than every other mainstream
> Mastodon client, found by diffing against `mastodon/mastodon-android`'s
> fragment/API layout.

- [x] **Custom emoji rendering**
  - `Instance` / `Status` / `Account` payloads carry a `emojis` array
    (`shortcode`, `url`, `static_url`). Currently rendered as literal
    `:shortcode:` text in `HtmlText`.
  - Parse `:shortcode:` runs in status bodies, display names, and bios into
    inline `Coil`-loaded images (`InlineTextContent` / `AsyncImage` in an
    `AnnotatedString`). Highest-visibility gap — shows up on almost every
    screen for instances with custom emoji.

- [x] **Lists**
  - Add `ListRepository` (`GET/POST/PUT/DELETE /api/v1/lists`,
    `GET/POST/DELETE /api/v1/lists/:id/accounts` for membership).
  - `ManageListsScreen` (create/rename/delete, replace-existing color/icon
    Mastodon doesn't support so keep it plain), `ListTimelineScreen` (reuses
    the `TimelineRepository`-style pattern against
    `GET /api/v1/timelines/list/:id`), and an "Add/remove from list" action
    reachable from `ProfileScreen`'s overflow menu.
  - Entry point: a "Lists" row in Settings or a new segmented pill in
    `TimelineScreen` alongside Home/Local/Federated.

- [x] **Keyword filters**
  - Add `FilterRepository` (`GET/POST/PUT/DELETE /api/v2/filters`, with
    nested `keywords`/`statuses` sub-resources).
  - `FiltersScreen` (list existing filters, create/edit with phrase, context
    checkboxes — home/notifications/public/thread/account — whole-word toggle,
    optional expiry).
  - Apply client-side: `StatusListPatch`/timeline rendering should check
    `Status.filtered` (populated server-side per `GET` request once filters
    exist) and collapse/hide matching statuses the same way CW does.

- [x] **Follow hashtag + manage followed hashtags**
  - Add `POST/POST /api/v1/tags/:tag/follow|unfollow` to (new or existing)
    a small `TagRepository`; `GET /api/v1/followed_tags` for the list screen.
  - Add a follow/unfollow toggle to `HashtagTimelineScreen`'s top bar.
  - `ManageFollowedHashtagsScreen` reachable from Settings, reusing
    `AsyncList`.

- [x] **Featured hashtags on profile**
  - `GET /api/v1/accounts/:id/featured_tags` — render as a chip row under the
    bio in `ProfileScreen` (own profile: manage via
    `POST/DELETE /api/v1/featured_tags`), tapping a chip opens
    `HashtagTimelineScreen`.

- [x] **Pinned posts on profile**
  - `GET /api/v1/accounts/:id/statuses?pinned=true` — prepend to
    `ProfileScreen`'s status list with a small "📌 Pinned" label on the card,
    matching every other Mastodon client's profile header.
  - Own-profile pin/unpin action: `POST /api/v1/statuses/:id/pin|unpin`,
    surfaced in the status context menu.

- [x] **Status edit history**
  - `GET /api/v1/statuses/:id/history` — when a status shows the "edited"
    timestamp (`Status.editedAt`, already decoded), make it tappable into a
    simple diff-less list of prior versions (text + media per revision).

- [x] **Notification requests (filtered notifications)**
  - Mastodon 4.3+: `GET /api/v1/notifications/requests`,
    `POST /api/v1/notifications/requests/:id/accept|dismiss`. Notifications
    from accounts you don't follow can be held back into a request queue
    instead of the main feed.
  - Add a "Requests" entry point at the top of `NotificationsScreen` when the
    count is non-zero; reuse `AsyncList` for the request list.

- [x] **Granular settings**
  - Default post visibility + default language for new toots
    (`Settings*Fragment` in the official app / `source` prefs on
    `update_credentials`) — read/write via `AccountRepository.updateCredentials`.
  - Per-type notification toggles (favourites/boosts/follows/mentions/polls)
    — Mastodon exposes these both server-side (push subscription `data[alerts]`
    flags, already partially wired in `PushRepository`) and should be surfaced
    as switches in `SettingsScreen`.
  - Link to the connected instance's about/rules page
    (`GET /api/v1/instance` → `rules`) — small "Server info" row in Settings.

---

## Milestone 6 — Release Prep
> **Estimate: 1–2 days**
> Goal: app can be published to the Play Store.

- [ ] **Release signing config**
  - Generate a keystore, add `signingConfigs.release` in `app/build.gradle.kts`.
  - Store keystore path + passwords in `local.properties` (gitignored).

- [ ] **ProGuard / R8 rules audit**
  - `proguard-rules.pro` is currently a skeleton.
  - Add keep rules for kotlinx.serialization, OkHttp, Coil, Room, Hilt, Navigation 3,
    and any reflection-heavy libs.

- [ ] **App version + versionCode**
  - Set meaningful `versionName` / `versionCode` in `app/build.gradle.kts`.
  - Wire a CI step (GitHub Actions or equivalent) for release builds.

- [ ] **Play Store assets**
  - Hi-res icon (512×512), feature graphic (1024×500), 2-8 screenshots per form factor.
  - Short + full description, privacy policy URL.

- [ ] **Privacy policy**
  - The app handles OAuth tokens and profile data.
  - Host a minimal privacy policy page and link it in the Play Store listing and Settings.

---

## Post-MVP Backlog

These are desirable but intentionally deferred past the initial release:

| Feature | Notes |
|---------|-------|
| **Streaming (WebSocket)** | Real-time timeline + notification updates via `GET /api/v1/streaming` |
| **Multi-account support** | Currently one credential set in DataStore |
| **Image alt-text viewer** | Show `MediaAttachment.description` on long-press |
| **Profile media tab** | `onlyMedia=true` param exists in `AccountRepository` but no UI tab |
| **Local search history** | Persist recent searches in DataStore / Room |
| **Haptic feedback** | On favourite / boost animations |
| **Toot language picker** | Per-toot `language` param on `POST /api/v1/statuses` (default-language setting is in Milestone 5; a per-toot override picker is lower priority) |
| **Animated GIF support** | Coil supports it; needs explicit `ImageLoader` config |
| **Follow request management** | Accept / reject from Notifications screen |
| **Quote posts** | Newer Mastodon (4.4+) quote-post feature (`StatusQuotesFragment` in the official app) — depends on server-side adoption, not yet universal |
| **Announcements** | Instance-wide banner (`GET /api/v1/announcements`, with reactions) shown in the official app's Home fragment |
| **Read markers** | `GET/POST /api/v1/markers` — sync last-read position across devices; invisible/QoL, not a user-facing gap |
| **Server info / rules screen (detail view)** | Beyond the Settings link in Milestone 5 — full about page with extended description, contact account, rules list with numbering |
