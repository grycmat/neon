package com.gigapingu.neon.feature.widget

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.core.content.edit
import androidx.glance.appwidget.updateAll
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.data.NotificationRepository
import com.gigapingu.neon.core.data.SettingsRepository
import com.gigapingu.neon.core.data.ThemeMode
import com.gigapingu.neon.core.designsystem.theme.NeonPalette
import com.gigapingu.neon.core.designsystem.util.htmlToPlainText
import com.gigapingu.neon.core.designsystem.util.relativeTime
import com.gigapingu.neon.core.model.MastoNotification
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Everything the widget needs from the data layer: build a [WidgetSnapshot] to draw, and pull a
 * fresh page from the instance.
 *
 * Reads go through the Room cache ([NotificationRepository.cachedNotifications]) rather than the
 * in-memory `state`, because the widget is routinely composed in a process the system started for
 * a broadcast, where no ViewModel ever ran a load.
 */
@Singleton
class NotificationWidgetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository,
) {
    internal companion object {
        /**
         * Rows fetched and rendered. Each carries a composited avatar bitmap and the whole widget
         * has to cross a Binder transaction, so this is a memory budget, not a layout choice — the
         * list scrolls, but off-screen rows still cost.
         */
        const val MAX_ROWS = 10

        /** How old the cache may get before a draw is allowed to block on a fetch. */
        private val STALE_AFTER = 5.minutes

        /** Ceiling on that fetch, so a dead network can't hold up the first draw. */
        private val COLD_FETCH_TIMEOUT = 10.seconds

        private const val PREFS = "neon_widget"
        private const val KEY_LAST_REFRESH = "last_refresh_at"
        private const val TAG = "NeonWidget"
    }

    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    /** Resolves the current state of the widget, including decoded avatars. Never throws. */
    internal suspend fun snapshot(): WidgetSnapshot {
        val palette = if (resolveDarkTheme()) NeonPalette.Dark else NeonPalette.Light
        if (!authRepository.ensureConfigured()) {
            return WidgetSnapshot(palette, WidgetContent.SignedOut, updatedLabel = "")
        }
        val lastRefresh = prefs.getLong(KEY_LAST_REFRESH, 0L)
        val notifications = runCatching { notificationRepository.cachedNotifications(MAX_ROWS) }
            .getOrElse {
                Log.w(TAG, "Could not read cached notifications", it)
                emptyList()
            }
        val content = if (notifications.isEmpty() && lastRefresh == 0L) {
            // Nothing cached and nothing ever fetched — a real "not yet", not "nothing to show".
            WidgetContent.Loading
        } else {
            WidgetContent.Ready(notifications.map { it.toRow(palette) })
        }
        return WidgetSnapshot(palette, content, updatedLabel(lastRefresh))
    }

    /**
     * Cold-path fetch, called from `provideGlance` before the first draw — the widget being
     * composed at all (first placement, reboot, the `updatePeriodMillis` tick) is the only signal
     * available when the app isn't running.
     *
     * Deliberately does not redraw: `provideGlance` is about to draw anyway, and calling
     * `updateAll` from inside it would recurse. The staleness gate is also what stops a redraw
     * triggered by [refresh] from turning into a second fetch.
     */
    internal suspend fun refreshIfStale() {
        val age = System.currentTimeMillis() - prefs.getLong(KEY_LAST_REFRESH, 0L)
        if (age < STALE_AFTER.inWholeMilliseconds) return
        withTimeoutOrNull(COLD_FETCH_TIMEOUT) { fetchAndPersist() }
    }

    /**
     * Pulls the newest notifications and redraws. This is the live path — a push arriving, or the
     * refresh button — so it ignores staleness and always fetches. Silent on failure: the widget
     * keeps showing whatever was cached.
     */
    suspend fun refresh() {
        fetchAndPersist()
        redraw()
    }

    /** Re-composes every placed widget from already-persisted data. */
    suspend fun redraw() {
        runCatching { NotificationWidget().updateAll(context) }
            .onFailure { Log.w(TAG, "Widget redraw failed", it) }
    }

    private suspend fun fetchAndPersist() {
        if (!authRepository.ensureConfigured()) return
        runCatching { notificationRepository.refreshForWidget() }
            .onSuccess { prefs.edit { putLong(KEY_LAST_REFRESH, System.currentTimeMillis()) } }
            .onFailure { Log.w(TAG, "Widget refresh failed, keeping cached notifications", it) }
    }

    private suspend fun MastoNotification.toRow(palette: NeonPalette): WidgetRow {
        val look = look(palette)
        val status = status
        return WidgetRow(
            id = id,
            name = account.displayNameOrUsername,
            verb = look.verb,
            preview = status?.content
                ?.let { htmlToPlainText(it).replace('\n', ' ').trim() }
                .orEmpty(),
            time = relativeTime(createdAt),
            statusId = status?.display?.id,
            avatar = WidgetAvatars.render(context, account, look, palette),
        )
    }

    private suspend fun resolveDarkTheme(): Boolean =
        when (settingsRepository.themeMode.first()) {
            ThemeMode.Dark -> true
            ThemeMode.Light -> false
            // Widgets get no Compose `isSystemInDarkTheme` — read the launcher's configuration.
            // (Configuration.isNightModeActive is API 30; the uiMode mask works back to minSdk.)
            ThemeMode.System ->
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                    Configuration.UI_MODE_NIGHT_YES
        }

    private fun updatedLabel(lastRefresh: Long): String =
        if (lastRefresh == 0L) "" else "Updated ${relativeTime(Instant.ofEpochMilli(lastRefresh))} ago"
}
