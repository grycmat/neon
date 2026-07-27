# Material You — implementation notes

Status: **implemented**. Themed launcher icon (monochrome adaptive-icon layer) was already in
place before this work. This document covers the dynamic-color half.

## What Material You is

Dynamic color (Android 12+ / API 31 `S`) extracts a seed color from the user's wallpaper and
exposes it as a tonal palette. Compose Material 3 wraps it as `dynamicDarkColorScheme(context)` /
`dynamicLightColorScheme(context)` — full `ColorScheme`s tuned for contrast on dark/light
surfaces respectively.

## Why a straight `dynamicColorScheme()` swap wouldn't have done anything visible

Neon's brand identity — the pink→purple→cyan gradient, glass surfaces, avatar colors — lives in
`NeonPalette` (`core/designsystem/.../theme/NeonPalette.kt`) and is read directly via
`LocalNeonPalette`/`NeonTheme.palette`, not through `MaterialTheme.colorScheme`. Swapping the
`ColorScheme` fed to `MaterialTheme` would only have touched the few roles Neon doesn't already
override for stock M3 widgets (sheets, snackbars, menus) — none of the actual brand gradient.

## What was built instead: palette-level dynamic color

`NeonPalette.dynamic(context, isLight)` (companion function, `NeonPalette.kt`) produces a full
`NeonPalette` by copying the static `Dark`/`Light` instance (so every glass/substrate/text value
is untouched) and overriding only the accent-derived fields:
`pinkInk/purpleInk/cyanInk`, `label`, `tintFill/tintBorder`, `pinkFill/pinkBorder`, `glow`,
`orbColors`, `avatarGradients`, `gradientColors`, and the new `accentPink/accentPurple/accentCyan/
accentPinkDim/accentCyanSoft` fields (added to the `NeonPalette` data class specifically to back
this — `gradientSoft`/`mediaMagenta`/`mediaCyan` were changed from hardcoded `NeonAccents.*` reads
to reading these instance fields, so they follow dynamic mode too).

Color derivation, since a stock Material You palette only has two genuinely distinct hues
(`primary`/`secondary` share a hue at different chroma; `tertiary` is the other one):
- **"voltage" trio** (full-saturation — gradients/avatars/orbs): `primary`/`tertiary` from
  `dynamicDarkColorScheme(context)` (Android already tunes these for vividness on a dark surface),
  with the middle "purple" stop as the midpoint (`lerp`) between them — there's no third
  wallpaper-derived hue to use.
- **"ink" trio** (deepened — hashtags/labels on light surfaces): same roles from
  `dynamicLightColorScheme(context)`.
- Light-mode gradients/orbs lighten the voltage trio toward white (`.15f`/`.35f`) rather than using
  ink, since deepened ink stretched across a gradient or avatar reads muddy.
- Falls back to the static `Dark`/`Light` palette below API 31.

`NeonTheme(darkTheme, dynamicColor = false, content)` (`core/designsystem/.../theme/NeonTheme.kt`)
picks `NeonPalette.dynamic(...)` over the static palette when `dynamicColor` is true and
`SDK_INT >= S`. `neonColorScheme()` needed no changes — it already derives the `MaterialTheme`
`ColorScheme` from whichever `NeonPalette` it's given, so it automatically follows too.

## Wiring (opt-in, off by default)

Settings → wallpaper icon persistence, mirroring the existing `twoPaneEnabled` pattern:
- `SettingsRepository.dynamicColorEnabled` (`core/data`) — `DataStore` boolean, default `false`.
- `ShellViewModel.dynamicColorEnabled` (`app`) — read-only, feeds `MainActivity` →
  `NeonTheme(dynamicColor = ...)`.
- `SettingsViewModel.dynamicColorEnabled` / `setDynamicColorEnabled` (`feature/settings`) — backs
  the Settings screen toggle.
- `SettingsScreen.kt` — "Match wallpaper colors" `Switch` under the theme-mode row, gated on
  `Build.VERSION.SDK_INT >= S` (hidden entirely below API 31, no dead toggle).

Default is off: the brand palette is what a fresh install shows; dynamic color is something a user
opts into per-device, same as any other appearance setting.

## Known limitations (accepted, not bugs)

- No live wallpaper-change updates without an app restart — Compose reads the dynamic scheme once
  per `NeonTheme` recomposition key (`darkTheme`, `dynamicColor`, `context`), and Android doesn't
  push a recomposable signal on wallpaper change. Matches how most dynamic-color reference
  implementations behave.
- The "purple" midpoint is synthetic (`lerp`), not a real third wallpaper hue — unavoidable given
  Material You's two-hue palette structure; it still produces a coherent 3-stop gradient.

## Elevation glow shadows now follow it too

`NeonAvatar.kt`, `Glass.kt` (`GradientButton`), and `HomeShell.kt` (the FAB in both the phone tab
bar and `ComposeFab`) each set a purple `ambientColor`/`spotColor` on a `Modifier.shadow` for their
glow. These were originally hardcoded to `NeonAccents.Purple`; they now read `palette.accentPurple`
instead, so the glow tint tracks dynamic color the same as everything else. `NeonAvatar`'s
no-image gradient fallback (`Brush.linearGradient(listOf(NeonAccents.Pink, NeonAccents.Purple))`)
was switched the same way, to `palette.accentPink`/`palette.accentPurple`. `NeonAccents` itself is
now only referenced from `NeonPalette.kt`, where it remains the source of the static brand trio.
