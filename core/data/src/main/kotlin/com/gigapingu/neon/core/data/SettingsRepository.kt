package com.gigapingu.neon.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gigapingu.neon.core.data.push.NotificationAlertPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** App theme mode. The design's primary palette is neon dark. */
enum class ThemeMode { Dark, Light, System }

private val Context.settingsStore by preferencesDataStore(name = "neon_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")
    private val twoPaneEnabledKey = booleanPreferencesKey("two_pane_enabled")
    private val dynamicColorEnabledKey = booleanPreferencesKey("dynamic_color_enabled")
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
        prefs[notificationsEnabledKey] ?: true
    }

    /** Big-screen list-detail/two-pane layout, on by default; off falls back to phone-style single-pane. */
    val twoPaneEnabled: Flow<Boolean> = context.settingsStore.data.map { prefs ->
        prefs[twoPaneEnabledKey] ?: true
    }

    /** Material You: derive the neon gradient/avatar/accent colors from the wallpaper (Android 12+). Off by default — the brand palette is the default identity. */
    val dynamicColorEnabled: Flow<Boolean> = context.settingsStore.data.map { prefs ->
        prefs[dynamicColorEnabledKey] ?: false
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

    suspend fun setTwoPaneEnabled(enabled: Boolean) {
        context.settingsStore.edit { prefs ->
            prefs[twoPaneEnabledKey] = enabled
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.settingsStore.edit { prefs ->
            prefs[dynamicColorEnabledKey] = enabled
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
