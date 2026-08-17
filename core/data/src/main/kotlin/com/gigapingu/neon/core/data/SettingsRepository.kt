package com.gigapingu.neon.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gigapingu.neon.core.data.push.NotificationAlertPrefs
import com.gigapingu.neon.core.data.push.PushDistributorStatus
import com.gigapingu.neon.core.data.push.PushEndpointProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** App theme mode. The design's primary palette is neon dark. */
enum class ThemeMode { Dark, Light, System }

/** Big-screen tab body layout: phone-style single pane, hinge-aligned list-detail, or a list-detail grid. */
enum class BigScreenLayout { List, TwoPane, Grid }

private val Context.settingsStore by preferencesDataStore(name = "neon_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pushEndpointProvider: PushEndpointProvider,
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")
    private val notificationPermissionRequestedKey = booleanPreferencesKey("notification_permission_requested")
    // Legacy on/off key, read as a fallback when bigScreenLayoutKey hasn't been written yet.
    private val twoPaneEnabledKey = booleanPreferencesKey("two_pane_enabled")
    private val bigScreenLayoutKey = stringPreferencesKey("big_screen_layout")
    private val dynamicColorEnabledKey = booleanPreferencesKey("dynamic_color_enabled")
    private val dismissedUpdateVersionKey = intPreferencesKey("dismissed_update_version")
    private object AlertKeys {
        val mention = booleanPreferencesKey("alert_mention")
        val favourite = booleanPreferencesKey("alert_favourite")
        val reblog = booleanPreferencesKey("alert_reblog")
        val follow = booleanPreferencesKey("alert_follow")
        val followRequest = booleanPreferencesKey("alert_follow_request")
        val poll = booleanPreferencesKey("alert_poll")
        val status = booleanPreferencesKey("alert_status")
        val update = booleanPreferencesKey("alert_update")
    }

    val themeMode: Flow<ThemeMode> = context.settingsStore.data.map { prefs ->
        when (prefs[themeModeKey]) {
            "light" -> ThemeMode.Light
            "system" -> ThemeMode.System
            else -> ThemeMode.Dark
        }
    }

    val notificationsEnabled: Flow<Boolean> = context.settingsStore.data.map { prefs ->
        prefs[notificationsEnabledKey] ?: (pushEndpointProvider.getDistributorStatus() !is PushDistributorStatus.NotInstalled)
    }

    /** Whether the POST_NOTIFICATIONS OS permission dialog has ever been shown to completion. */
    val notificationPermissionRequested: Flow<Boolean> = context.settingsStore.data.map { prefs ->
        prefs[notificationPermissionRequestedKey] ?: false
    }

    /** Big-screen tab body layout, list-detail by default; falls back to the legacy on/off key pre-migration. */
    val bigScreenLayout: Flow<BigScreenLayout> = context.settingsStore.data.map { prefs ->
        when (prefs[bigScreenLayoutKey]) {
            "list" -> BigScreenLayout.List
            "grid" -> BigScreenLayout.Grid
            "two_pane" -> BigScreenLayout.TwoPane
            else -> if (prefs[twoPaneEnabledKey] == false) BigScreenLayout.List else BigScreenLayout.TwoPane
        }
    }

    /** Material You: derive the neon gradient/avatar/accent colors from the wallpaper (Android 12+). Off by default — the brand palette is the default identity. */
    val dynamicColorEnabled: Flow<Boolean> = context.settingsStore.data.map { prefs ->
        prefs[dynamicColorEnabledKey] ?: false
    }

    /**
     * The `availableVersionCode` the user declined in the flexible in-app-update flow, so we don't
     * re-offer the same build on every launch. `0` means nothing has been declined.
     */
    val dismissedUpdateVersion: Flow<Int> = context.settingsStore.data.map { prefs ->
        prefs[dismissedUpdateVersionKey] ?: 0
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsStore.edit { prefs ->
            prefs[themeModeKey] = when (mode) {
                ThemeMode.Light -> "light"
                ThemeMode.System -> "system"
                ThemeMode.Dark -> "dark"
            }
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsStore.edit { prefs ->
            prefs[notificationsEnabledKey] = enabled
        }
    }

    suspend fun setNotificationPermissionRequested(requested: Boolean) {
        context.settingsStore.edit { prefs ->
            prefs[notificationPermissionRequestedKey] = requested
        }
    }

    suspend fun setBigScreenLayout(layout: BigScreenLayout) {
        context.settingsStore.edit { prefs ->
            prefs[bigScreenLayoutKey] = when (layout) {
                BigScreenLayout.List -> "list"
                BigScreenLayout.TwoPane -> "two_pane"
                BigScreenLayout.Grid -> "grid"
            }
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.settingsStore.edit { prefs ->
            prefs[dynamicColorEnabledKey] = enabled
        }
    }

    suspend fun setDismissedUpdateVersion(versionCode: Int) {
        context.settingsStore.edit { prefs ->
            prefs[dismissedUpdateVersionKey] = versionCode
        }
    }

    val notificationAlertPrefs: Flow<NotificationAlertPrefs> = context.settingsStore.data.map { prefs ->
        NotificationAlertPrefs(
            mention = prefs[AlertKeys.mention] ?: true,
            favourite = prefs[AlertKeys.favourite] ?: true,
            reblog = prefs[AlertKeys.reblog] ?: true,
            follow = prefs[AlertKeys.follow] ?: true,
            followRequest = prefs[AlertKeys.followRequest] ?: true,
            poll = prefs[AlertKeys.poll] ?: true,
            status = prefs[AlertKeys.status] ?: true,
            update = prefs[AlertKeys.update] ?: true,
        )
    }

    suspend fun setNotificationAlertPrefs(prefs: NotificationAlertPrefs) {
        context.settingsStore.edit { store ->
            store[AlertKeys.mention] = prefs.mention
            store[AlertKeys.favourite] = prefs.favourite
            store[AlertKeys.reblog] = prefs.reblog
            store[AlertKeys.follow] = prefs.follow
            store[AlertKeys.followRequest] = prefs.followRequest
            store[AlertKeys.poll] = prefs.poll
            store[AlertKeys.status] = prefs.status
            store[AlertKeys.update] = prefs.update
        }
    }
}
