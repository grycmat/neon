# Phase 1 — User feedback fixes

Source: `issues.md` (Polish-language user feedback). Positive feedback (color
intensity control, speed, working push) needs no action and isn't itemized
below. Each item lists: the report, root cause with file:line, and the fix.

## P0 — Broken core functionality

### 1. Composer character limit ignores the instance's real limit — ✅ RESOLVED
**Report** (#4, #8, #9): app shows a 500-char limit regardless of the
instance's actual configured limit (one user's instance allows 10,000); the
counter can be driven past the limit ("592/500") and once over, Post is
silently disabled with no way to recover other than manually deleting text.

**Root cause**:
- `feature/composer/.../ComposeViewModel.kt:32` — `const val MAX_CHARS = 500`,
  a hardcoded constant, never read from the server.
- There is no `Instance` model or repository anywhere in the codebase — the
  app never calls `/api/v1/instance` or `/api/v2/instance`.
- `ComposeUiState.canPost` (`ComposeViewModel.kt:59`) just checks
  `text.length <= MAX_CHARS`; `onTextChange` (`ComposeViewModel.kt:198-202`)
  never clamps input, so `textField.text.length` can freely exceed the limit.

**Fix**:
1. Add `core/model/Instance.kt` (minimal: `maxStatusCharacters: Int`, with a
   safe default of 500) and `core/data/InstanceRepository.kt` — singleton,
   fetches `GET /api/v2/instance` (falls back to `/api/v1/instance` /
   `max_toot_chars` for older servers), caches the result for the session
   (instance limits don't change mid-session; a `CacheStore` entity-cache
   entry keyed by instance host is fine for cross-launch reuse).
2. Inject `InstanceRepository` into `ComposeViewModel`; replace the
   `MAX_CHARS` constant with a `maxChars` value from the repository (falls
   back to 500 while the fetch is in flight or on failure).
3. Clamp in `onTextChange`: if the new value's length would be accepted,
   still allow typing past the limit (Mastodon UX convention shows negative
   remaining count rather than hard-blocking keystrokes), but fix the actual
   bug — `canPost` already correctly disables Post over the limit; the
   reported "bug" is really issue (1), the limit itself being wrong. Once the
   real per-instance limit is wired in, 592 characters will correctly show as
   under a 10,000 limit and Post will work. No separate overflow-prevention
   logic needed once the limit is accurate.

**Resolved**: Added `core/model/Instance.kt` and `core/data/InstanceRepository.kt`
(fetches `/api/v2/instance`, falls back to `/api/v1/instance`'s `max_toot_chars`,
caches in memory for the session, cleared on logout via `AuthRepository`).
`ComposeViewModel`/`ComposeUiState` now carry a `maxChars` field sourced from
the repository instead of the hardcoded `MAX_CHARS` constant; `ComposeScreen`'s
counter and `canPost` gate both use it.

### 2. Video attachments can't be picked or uploaded ✅ RESOLVED
**Report** (#14b): can't attach a video to a toot; the app doesn't show
videos in the gallery picker at all.

**Root cause**: `feature/composer/.../ComposeScreen.kt:272-277` launches the
photo picker with
`PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)` —
videos are filtered out of the picker UI by request, not by any deliberate
product decision. Secondary issue: `core/network/.../ApiClient.kt:94-102`
`multipart()` hardcodes every uploaded part's content type to
`application/octet-stream` instead of the real MIME type.

**Fix**:
1. `ComposeScreen.kt:274` — change `ImageOnly` to `ImageAndVideo`.
2. `ApiClient.kt` multipart builder — resolve the real MIME type via
   `ContentResolver.getType(uri)` (already available where the URI is read in
   `ComposeViewModel.readUri`, `ComposeViewModel.kt:257-268`) and pass it
   through to the multipart part instead of the hardcoded octet-stream type,
   so Mastodon's server-side type detection/limits work correctly for video.
3. Sanity-check `MediaRepository.upload` / composer UI for any image-only
   assumptions in the attachment preview (thumbnail rendering) — if the
   preview code assumes a `Bitmap`-decodable image, it'll need a video
   thumbnail/icon fallback. (Verify during implementation; not confirmed as
   broken by research, just adjacent risk.)

### 3. Push notification permission dialog reappears every launch ✅ RESOLVED
**Report** (#12): the app asks to enable notifications every time it's
opened.

**Root cause**: `app/.../MainActivity.kt:32-44` (`onCreate`) unconditionally
calls `ActivityCompat.requestPermissions(...)` whenever
`POST_NOTIFICATIONS` isn't currently granted — there's no persisted "already
asked" / "user declined" state, no `shouldShowRequestPermissionRationale`
check, and it's a raw legacy `requestPermissions` call rather than a
`registerForActivityResult` launcher with a denial callback.

**Fix**:
1. Add a `notificationPermissionRequested: Boolean` flag to
   `SettingsRepository`'s DataStore (or a lightweight dedicated prefs key).
2. Replace the raw `ActivityCompat.requestPermissions` call with
   `registerForActivityResult(ActivityResultContracts.RequestPermission())`;
   on the result callback, persist `notificationPermissionRequested = true`
   regardless of grant/deny.
3. In `onCreate`, only launch the request when permission is not granted
   **and** the flag is not yet set (i.e. ask once). Optionally surface a
   manual "Enable notifications" entry point in Settings (there's likely
   already a notifications toggle per the CLAUDE.md settings description) for
   users who declined once but change their mind — Android won't let the app
   re-prompt automatically after a denial anyway, so this is also the
   correct long-term UX regardless of the repeat-prompt bug.

### 4. Notification badge/system-notification never clears after reading
**Report** (#14a): the notification badge doesn't disappear after reading
the post.

**Root cause**: There is no in-app unread-count concept for the Notifications
tab at all (confirmed by grepping for badge/unread/markAsRead across
`HomeShell.kt`, `ShellViewModel.kt`, `NotificationRepository.kt`,
`feature/notifications/`). What the user sees is the **Android system
notification / launcher badge**, posted in
`feature/notifications/.../PushMessageHandler.kt:108-116` with
`.setAutoCancel(true)`. That only clears the shade entry (and its badge
contribution) if the user taps that specific system notification — opening
the app directly, or opening the in-app Notifications tab, never calls
`NotificationManagerCompat.cancel()`/`cancelAll()`. `NotificationRepository`
already has `dismiss(id)` (`NotificationRepository.kt:103`, calls
`/api/v1/notifications/:id/dismiss`) and `clear()` (line 112, calls
`/api/v1/notifications/clear`), but neither is wired to cancel the local
Android notification, and neither fires automatically when the feed is
viewed.

**Fix**:
1. In `NotificationsViewModel.init` (where `notifications.load()` currently
   runs), also call `NotificationManagerCompat.from(context).cancelAll()`
   (scoped to the `neon_notifications` channel's posted IDs, or just
   `cancelAll()` if the app doesn't post unrelated notification types) once
   the feed has loaded — this clears the OS badge/shade as soon as the user
   views the tab, matching Mastodon official-app behavior.
2. Also cancel on `MainActivity.handleNotificationIntent` /
   `Navigator.handleNotificationClick` (`core/ui/.../Navigator.kt:85`) for
   the specific tapped notification's ID, in case `setAutoCancel` timing
   races with the deep-link navigation.

## P1 — Composer layout & UX

### 5. Post button barely fits / gets squeezed
**Report** (#5): the send-toot button practically doesn't fit on screen.

**Root cause**: `ComposeScreen.kt:263-323` — the bottom toolbar `Row` packs
5 fixed `44.dp` `GlassIconButton`s, a flexible spacer, the char-count text,
and a `GradientButton` fixed to `Modifier.width(96.dp)`, with nothing allowed
to shrink or wrap. Fixed-width content alone approaches or exceeds a
360–412dp phone width. This gets worse once fix #1 lands, since a 5-digit
counter (`"9821/10000"`) is wider than `"492/500"`.

**Fix**: Rework the toolbar row so the char-count `Text` and `GradientButton`
never get clipped — e.g. drop the fixed `96.dp` width on the Post button
(let it size to content with fixed horizontal padding instead), and/or move
the 5 formatting icon buttons into a horizontally scrollable sub-row so they
never compete with the counter/button for space. Verify at a 360dp width
(smallest common Android device class) once the real (larger) character
counts from fix #1 are in place.

### 6. Composer text field doesn't scroll while typing ✅ RESOLVED
**Report** (#6): the text field doesn't scroll as you type, so the caret can
end up off-screen.

**Root cause**: `ComposeScreen.kt:158-163` puts `verticalScroll` on the
*outer* `Column`, and the toot body `BasicTextField`
(`ComposeScreen.kt:191-214`) has no `BringIntoViewRequester` tied to caret
movement/text changes — the outer scroll only reacts to focus-gain, not to
every keystroke as content grows past the viewport.

**Fix**: Add a `BringIntoViewRequester` to the body `BasicTextField`, and
call `bringIntoViewRequester.bringIntoView()` inside a `LaunchedEffect` keyed
on the text field's cursor/selection position (or simply on every
`onValueChange`), scoped within a coroutine on the existing outer scroll
state. This is the standard Compose pattern for "keep caret visible in a
scrollable ancestor."

## P1 — Thread view

### 7. Can't tell where one reply thread ends and another begins ✅ RESOLVED
**Report** (#7): looking at replies to a toot, hard to tell where a new
reply subthread starts vs. ends.

**Root cause**: `feature/thread/.../ThreadScreen.kt:267-280`
(`replyItems()`) renders `descendants` as a single flat `LazyColumn` of
`StatusCard`s in raw server order, with no use of `Status.inReplyToId` to
reconstruct branch structure. Every descendant — whether a direct reply to
the focused toot or several levels deep into one specific branch — gets
identical treatment with no divider, indentation, or grouping. (Ancestors →
focused → descendants sections *are* visually distinct via `FocusedStatus()`,
`ThreadScreen.kt:330-401` — this issue is specifically about sibling
branches within the descendants section.)

**Fix**: Reconstruct a shallow reply tree from `descendants` using
`inReplyToId` in `ThreadViewModel` (build parent→children groups, walk
depth-first from each direct child of the focused status). In
`ThreadScreen.replyItems()`, use the resulting depth to:
- add a small left indent per depth level (capped at ~2-3 levels to avoid
  runaway nesting on phone widths), and
- insert a visual separator (subtle divider or extra spacing) between
  top-level sibling branches (depth-0 items whose predecessor was part of a
  different branch).

This mirrors the "ancestors → focused → replies" structuring already used
for the coarse sections, applied one level deeper. Keep it lightweight — no
need for a full collapsible tree UI, just enough visual grouping to answer
"is this reply part of the thread I was just reading, or a new one."

## P2 — Content fidelity & discoverability

### 8. Markdown doesn't render; single newlines get lost
**Report** (#10, part 1): Markdown styling doesn't work; a single newline
gets swallowed and text runs together.

**Root cause**:
- `core/data/.../StatusRepository.kt` `create()`/`edit()` (lines ~111-194)
  never send a `content_type` field (e.g. `"text/markdown"`), so even on
  instances that support markdown source via `source[content_type]`
  (Glitch-soc/Pleroma/Iceshrimp forks), the client never opts in — the
  instance's default formatter treats `**bold**` as literal text.
- `core/designsystem/.../util/Html.kt:43-78` (`parseStatusHtml`) only
  converts a returned `<br>` to `\n` and a `</p><p>` boundary to `\n\n`;
  any other paragraph tag is stripped with no separator. If the server's
  formatter doesn't emit `<br>` for a plain-text single newline (likely,
  since no markdown/content-type opt-in is sent), the line break is lost on
  round-trip.
- `core/designsystem/.../component/HtmlText.kt:59-98` has no markdown-specific
  span rendering (bold/italic) — confirms there's no client-side markdown
  interpretation either.

**Fix** (scope this carefully — full markdown support is a bigger feature
than the newline bug):
1. Newline fix (the higher-value, lower-risk half): most vanilla Mastodon
   instances auto-`<p>`-wrap text and convert single `\n` within a paragraph
   to `<br>` server-side already — so the likely actual gap is in
   `parseStatusHtml`'s regex coverage of paragraph/line-break variants
   returned by different server implementations (Mastodon vs Pleroma/Akkoma
   vs Glitch-soc format HTML slightly differently). Audit real API responses
   from a couple of instance types and broaden the regexes in `Html.kt:43-48`
   accordingly, rather than assuming content-type is the fix.
2. Markdown: only pursue if targeting markdown-capable forks explicitly —
   add an opt-in `content_type` field to the compose request (gated on
   instance capability, ideally sourced from the same `InstanceRepository`
   added in fix #1, since plain vanilla Mastodon does not support markdown
   input at all and would show raw asterisks if sent). This is a genuine new
   feature, not a bug fix — recommend splitting it into its own follow-up
   rather than bundling with the newline fix.

### 9. Edit toot / pin to profile "don't work"
**Report** (#10, part 2): can't edit a toot, can't pin a toot to profile.

**Root cause**: Both are **already fully implemented end-to-end** —
`StatusRepository.edit()` (PUT `/api/v1/statuses/:id`) and
`StatusRepository.pin()` (POST `/api/v1/statuses/:id/pin`/`unpin`) both work,
wired through `StatusActionService` and reflected in `ProfileViewModel`. The
only entry point for either is the long-press context menu on a status card
(`StatusCard.kt` `StatusContextMenuSheet`, shown only for own statuses) —
there is no visible icon/affordance suggesting this menu exists, so users
don't discover it. This is a discoverability bug, not a missing-feature bug.

**Fix**: Add a visible overflow ("⋮") icon button to `StatusActions.kt`'s
action row (next to favourite/boost/reply) for the user's own statuses,
opening the same `StatusContextMenuSheet` that long-press already triggers —
no backend or ViewModel changes needed, just exposing the existing sheet
through a discoverable tap target instead of relying solely on long-press.

## Suggested sequencing

1. P0 items (1–4) — instance-aware char limit, video picker, permission
   prompt, notification badge. Independent of each other, can be done in
   parallel by area.
2. P1 items (5–7) — composer layout + thread visual grouping. #5 depends on
   #1 landing first (verify final layout against real large character
   counts).
3. P2 item 9 (edit/pin affordance) — small, isolated, do anytime.
4. P2 item 8 (markdown/newline) — needs the instance-response audit before
   committing to an approach; treat as its own spike, not a quick fix.
