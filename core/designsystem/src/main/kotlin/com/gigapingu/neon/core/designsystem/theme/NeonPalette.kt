package com.gigapingu.neon.core.designsystem.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Every color in the app lives here. Two palettes: Neon Midnight (dark) and
 * Neon Daylight (light) — the same glass architecture on an inverted substrate.
 *
 * Rules that keep the two schemes coherent:
 *  • the full-voltage accent trio survives untouched inside gradients, avatars
 *    and media, so brand recall holds in both schemes;
 *  • accents used as *ink* (hashtags, kickers, selected labels) are deepened in
 *    light mode so they clear ~4.5:1 on paper-bright ground;
 *  • glass fills invert: white-alpha over dark, ink-alpha over light;
 *  • the backdrop orbs are pulled back to a whisper in light mode.
 */
object NeonAccents {
    // Full-voltage trio — gradients, avatars, media only.
    val Pink = Color(0xFFFF2E8B)
    val Purple = Color(0xFF7B3DFF)
    val Cyan = Color(0xFF22E2FF)
    val PinkDim = Color(0xFFFF6EE5)

    // Deepened variants for accent-as-ink on light surfaces.
    val PinkInk = Color(0xFFE0116F)
    val PurpleInk = Color(0xFF6A2AE0)
    val CyanInk = Color(0xFF0B8FAB)
    val CyanSoft = Color(0xFF22C9EA)
}

data class NeonPalette(
    val isLight: Boolean,
    // Substrate & glass
    val bg: Color,
    val bg2: Color,
    val surface: Color,
    val surfaceHi: Color,
    val surfaceSolid: Color,
    val border: Color,
    val border2: Color,
    val borderStrong: Color,
    val divider: Color,
    // Fill ramp — hover, pressed, selected chip, track
    val fill1: Color,
    val fill2: Color,
    val fill3: Color,
    val fill4: Color,
    val fill5: Color,
    // Chrome
    val barBg: Color,
    val gestureBar: Color,
    val snackBg: Color,
    val scrim: Color,
    // Type
    val text: Color,
    val textStrong: Color,
    val textBody: Color,
    val textDim: Color,
    val textMute: Color,
    val label: Color,
    val onGradient: Color,
    val mediaCaption: Color,
    // Accent-as-ink (hashtags, active icons, counters)
    val pinkInk: Color,
    val purpleInk: Color,
    val cyanInk: Color,
    // Accent-as-voltage (gradients, avatars, orbs). Static palettes hold the brand trio;
    // Material You mode (`dynamic`) swaps these for wallpaper-derived hues.
    val accentPink: Color,
    val accentPurple: Color,
    val accentCyan: Color,
    val accentPinkDim: Color,
    val accentCyanSoft: Color,
    // Accent state layers
    val tintFill: Color,
    val tintBorder: Color,
    val pinkFill: Color,
    val pinkBorder: Color,
    // Depth & backdrop
    val shadow: Color,
    val shadowStrong: Color,
    val glow: Color,
    val orbOpacity: Float,
    val orbColors: List<Color>,
    val avatarGradients: List<List<Color>>,
    val gradientColors: List<Color>,
) {
    /** Aliases kept so existing call sites (`palette.pink`, `palette.cyan`) still resolve. */
    val pink: Color get() = pinkInk
    val purple: Color get() = purpleInk
    val cyan: Color get() = cyanInk

    val gradient: Brush get() = Brush.horizontalGradient(gradientColors)

    val gradientSoft: Brush
        get() = Brush.linearGradient(
            if (isLight) {
                listOf(accentPink.copy(alpha = .12f), accentPurple.copy(alpha = .12f))
            } else {
                listOf(accentPink.copy(alpha = .20f), accentPurple.copy(alpha = .20f))
            },
        )

    /** Media placeholder washes — `magenta` and `cyan` variants. */
    val mediaMagenta: Brush
        get() = Brush.linearGradient(
            if (isLight) {
                listOf(accentPink.copy(alpha = .16f), accentPurple.copy(alpha = .14f))
            } else {
                listOf(accentPink.copy(alpha = .28f), accentPurple.copy(alpha = .24f))
            },
        )

    val mediaCyan: Brush
        get() = Brush.linearGradient(
            if (isLight) {
                listOf(accentCyanSoft.copy(alpha = .16f), accentPurple.copy(alpha = .14f))
            } else {
                listOf(accentCyan.copy(alpha = .26f), accentPurple.copy(alpha = .24f))
            },
        )

    fun avatarGradient(seed: Int): Brush =
        Brush.linearGradient(avatarGradients[seed.mod(avatarGradients.size)])

    companion object {
        val Dark = NeonPalette(
            isLight = false,
            bg = Color(0xFF07060D),
            bg2 = Color(0xFF0D0B16),
            surface = Color(0x0BFFFFFF), // rgba(255,255,255,.045)
            surfaceHi = Color(0x12FFFFFF),
            surfaceSolid = Color(0xFF0D0B16),
            border = Color(0x1CFFFFFF),
            border2 = Color(0x26FFFFFF),
            borderStrong = Color(0x38FFFFFF),
            divider = Color(0x12FFFFFF),
            fill1 = Color(0x09FFFFFF),
            fill2 = Color(0x0DFFFFFF),
            fill3 = Color(0x11FFFFFF),
            fill4 = Color(0x16FFFFFF),
            fill5 = Color(0x1AFFFFFF),
            barBg = Color(0xB80D0B16),
            gestureBar = Color(0x38FFFFFF),
            snackBg = Color(0xEB0D0B16),
            scrim = Color(0x94040308),
            text = Color(0xFFF3F1F8),
            textStrong = Color(0xFFFFFFFF),
            textBody = Color(0xFFE7E3F0),
            textDim = Color(0xFF9A93AD),
            textMute = Color(0xFF6F6982),
            label = NeonAccents.Cyan,
            onGradient = Color.White,
            mediaCaption = Color(0xB8FFFFFF),
            pinkInk = NeonAccents.Pink,
            purpleInk = NeonAccents.Purple,
            cyanInk = NeonAccents.Cyan,
            accentPink = NeonAccents.Pink,
            accentPurple = NeonAccents.Purple,
            accentCyan = NeonAccents.Cyan,
            accentPinkDim = NeonAccents.PinkDim,
            accentCyanSoft = NeonAccents.CyanSoft,
            tintFill = Color(0x1722E2FF),
            tintBorder = Color(0x4D22E2FF),
            pinkFill = Color(0x1AFF2E8B),
            pinkBorder = Color(0x66FF2E8B),
            shadow = Color(0x47000000), // card lift
            shadowStrong = Color(0x99000000), // sheets, modals, the phone itself
            glow = Color(0x667B3DFF), // gradient buttons
            orbOpacity = .40f,
            orbColors = listOf(NeonAccents.Pink, NeonAccents.Purple, NeonAccents.Cyan),
            avatarGradients = listOf(
                listOf(NeonAccents.Pink, NeonAccents.Purple),
                listOf(NeonAccents.Purple, NeonAccents.Cyan),
                listOf(NeonAccents.Pink, NeonAccents.Cyan),
                listOf(NeonAccents.PinkDim, NeonAccents.Purple),
                listOf(NeonAccents.Cyan, NeonAccents.Purple),
            ),
            gradientColors = listOf(NeonAccents.Pink, NeonAccents.Purple, NeonAccents.Cyan),
        )

        val Light = NeonPalette(
            isLight = true,
            bg = Color(0xFFF6F4FA),
            bg2 = Color(0xFFFFFFFF),
            surface = Color(0xC7FFFFFF), // rgba(255,255,255,.78)
            surfaceHi = Color(0xEBFFFFFF),
            surfaceSolid = Color(0xFFFFFFFF),
            border = Color(0x1A181426), // ink-alpha, not white-alpha
            border2 = Color(0x29181426),
            borderStrong = Color(0x42181426),
            divider = Color(0x14181426),
            fill1 = Color(0x07181426),
            fill2 = Color(0x0B181426),
            fill3 = Color(0x0F181426),
            fill4 = Color(0x14181426),
            fill5 = Color(0x1C181426),
            barBg = Color(0xC7FFFFFF),
            gestureBar = Color(0x3D181426),
            snackBg = Color(0xF2FFFFFF),
            scrim = Color(0x57261A42),
            text = Color(0xFF171426),
            textStrong = Color(0xFF0E0C18),
            textBody = Color(0xFF312C42),
            textDim = Color(0xFF6A6480),
            textMute = Color(0xFF948DA8),
            label = NeonAccents.PurpleInk, // cyan is unreadable on white — kickers go violet
            onGradient = Color.White,
            mediaCaption = Color(0x9E181426),
            pinkInk = NeonAccents.PinkInk,
            purpleInk = NeonAccents.PurpleInk,
            cyanInk = NeonAccents.CyanInk,
            accentPink = NeonAccents.Pink,
            accentPurple = NeonAccents.Purple,
            accentCyan = NeonAccents.Cyan,
            accentPinkDim = NeonAccents.PinkDim,
            accentCyanSoft = NeonAccents.CyanSoft,
            tintFill = Color(0x1A0B8FAB),
            tintBorder = Color(0x570B8FAB),
            pinkFill = Color(0x17E0116F),
            pinkBorder = Color(0x61E0116F),
            shadow = Color(0x1732225A), // violet-tinted, softer than dark mode
            shadowStrong = Color(0x3832225A),
            glow = Color(0x477B3DFF),
            orbOpacity = .19f,
            orbColors = listOf(Color(0xFFFF8EC4), Color(0xFFB79BFF), Color(0xFF8FE4F5)),
            avatarGradients = listOf(
                listOf(Color(0xFFFF4D9C), Color(0xFF8452FF)),
                listOf(Color(0xFF8452FF), Color(0xFF2FCFEB)),
                listOf(Color(0xFFFF4D9C), Color(0xFF2FCFEB)),
                listOf(Color(0xFFFF7AE0), Color(0xFF8452FF)),
                listOf(Color(0xFF2FCFEB), Color(0xFF8452FF)),
            ),
            gradientColors = listOf(NeonAccents.Pink, NeonAccents.Purple, NeonAccents.CyanSoft),
        )

        /**
         * Material You variant: keeps every substrate/glass/text value from the static [Dark]/[Light]
         * palette untouched, and re-derives only the accent trio (gradients, avatars, orbs, ink) from
         * the device's wallpaper colors. Falls back to the static palette below API 31 or if the
         * dynamic color scheme can't be read.
         *
         * The "voltage" trio (full-saturation, for gradients/avatars/orbs) comes from
         * [dynamicDarkColorScheme]'s primary/tertiary roles — Android tunes those for contrast on a
         * dark surface, which is exactly the vividness the dark-mode brand accents want. The "ink"
         * trio (deepened, for hashtags/labels on light surfaces) comes from the same roles in
         * [dynamicLightColorScheme]. There's no third distinct hue in a stock Material You palette
         * (secondary shares primary's hue at lower chroma), so the middle "purple" stop is the
         * midpoint between pink and cyan rather than its own wallpaper-derived hue.
         */
        fun dynamic(context: Context, isLight: Boolean): NeonPalette {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return if (isLight) Light else Dark
            val base = if (isLight) Light else Dark

            val darkScheme = dynamicDarkColorScheme(context)
            val lightScheme = dynamicLightColorScheme(context)
            val voltage = AccentTrio(
                pink = darkScheme.primary,
                purple = lerp(darkScheme.primary, darkScheme.tertiary, .5f),
                cyan = darkScheme.tertiary,
            )
            val ink = AccentTrio(
                pink = lightScheme.primary,
                purple = lerp(lightScheme.primary, lightScheme.tertiary, .5f),
                cyan = lightScheme.tertiary,
            )
            // Dark-mode ink is just the raw voltage trio (already readable on a dark substrate);
            // light-mode gradients/orbs lighten voltage toward white instead of using ink (deepened
            // ink would look muddy stretched across a gradient or avatar).
            val gradientVoltage = if (isLight) voltage.lightened(.15f) else voltage
            val orbVoltage = if (isLight) voltage.lightened(.35f) else voltage
            val pink = if (isLight) ink.pink else voltage.pink
            val purple = if (isLight) ink.purple else voltage.purple
            val cyan = if (isLight) ink.cyan else voltage.cyan

            return base.copy(
                pinkInk = pink,
                purpleInk = purple,
                cyanInk = cyan,
                label = if (isLight) purple else voltage.cyan,
                tintFill = cyan.copy(alpha = if (isLight) .10f else .09f),
                tintBorder = cyan.copy(alpha = if (isLight) .34f else .30f),
                pinkFill = pink.copy(alpha = if (isLight) .09f else .10f),
                pinkBorder = pink.copy(alpha = if (isLight) .38f else .40f),
                glow = voltage.purple.copy(alpha = if (isLight) .28f else .40f),
                orbColors = listOf(orbVoltage.pink, orbVoltage.purple, orbVoltage.cyan),
                avatarGradients = listOf(
                    listOf(gradientVoltage.pink, gradientVoltage.purple),
                    listOf(gradientVoltage.purple, gradientVoltage.cyan),
                    listOf(gradientVoltage.pink, gradientVoltage.cyan),
                    listOf(lerp(gradientVoltage.pink, Color.White, .15f), gradientVoltage.purple),
                    listOf(gradientVoltage.cyan, gradientVoltage.purple),
                ),
                gradientColors = listOf(gradientVoltage.pink, gradientVoltage.purple, gradientVoltage.cyan),
                accentPink = voltage.pink,
                accentPurple = voltage.purple,
                accentCyan = voltage.cyan,
                accentPinkDim = lerp(voltage.pink, Color.White, .15f),
                accentCyanSoft = lerp(voltage.cyan, Color.White, .1f),
            )
        }
    }
}

private data class AccentTrio(val pink: Color, val purple: Color, val cyan: Color) {
    fun lightened(fraction: Float): AccentTrio = AccentTrio(
        pink = lerp(pink, Color.White, fraction),
        purple = lerp(purple, Color.White, fraction),
        cyan = lerp(cyan, Color.White, fraction),
    )
}
