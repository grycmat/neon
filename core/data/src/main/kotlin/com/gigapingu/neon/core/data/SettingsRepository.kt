package com.gigapingu.neon.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
}
