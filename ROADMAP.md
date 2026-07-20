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
| Push notifications | ❌ Missing |
| Video playback (Media3 / ExoPlayer) | ✅ Complete |
| Streaming (WebSocket) | ❌ Post-MVP |

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

- [ ] **Push notifications**
  - Register a push subscription with the Mastodon instance:
    `POST /api/v1/push/subscription` (Web Push RFC 8030).
  - Create a `NotificationService` / `BroadcastReceiver` that decrypts the payload and
    posts a system notification via `NotificationManager`.
  - Deep-link taps into `ThreadScreen` or `NotificationsScreen`.

- [ ] **Accessibility pass**
  - Add meaningful `contentDescription` to all icon-only buttons
    (`GlassIconButton`, `ComposeFab`, `StatusActions` buttons).
  - Add `semantics { role = Role.Button }` where appropriate.
  - Verify focus traversal order in `ComposeScreen` and `ProfileScreen`.

- [x] **Error retry button**
  - `AsyncList` already shows error text. Add a "Retry" `GlassButton` below it that calls
    the `onRefresh` callback.

- [ ] **Account fields in Edit Profile**
  - `AccountField` model and `fields_attributes` API wiring already exist in
    `AccountRepository.updateCredentials()`.
  - Complete the `EditProfileScreen` UI: dynamic list of name/value `TextField` pairs,
    add/remove field buttons, capped at 4.

---

## Milestone 5 — Release Prep
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
| **Custom emoji** | Mastodon `:shortcode:` inline images in bios + toots |
| **Haptic feedback** | On favourite / boost animations |
| **Toot language picker** | `language` param on `POST /api/v1/statuses` |
| **Animated GIF support** | Coil supports it; needs explicit `ImageLoader` config |
| **Follow request management** | Accept / reject from Notifications screen |
