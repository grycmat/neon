package com.gigapingu.neon.core.data

import android.content.Context
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

    val themeMode: Flow<ThemeMode> = context.settingsStore.data.map { prefs ->
        when (prefs[themeModeKey]) {
            "light" -> ThemeMode.Light
            "system" -> ThemeMode.System
            else -> ThemeMode.Dark
        }
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
}
