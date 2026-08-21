package com.gigapingu.neon.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AccountRepository
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.data.IconScale
import com.gigapingu.neon.core.data.SettingsRepository
import com.gigapingu.neon.core.data.TextScale
import com.gigapingu.neon.core.data.ThemeMode
import com.gigapingu.neon.core.data.push.NotificationAlertPrefs
import com.gigapingu.neon.core.data.push.PushDistributorStatus
import com.gigapingu.neon.core.data.push.PushEndpointProvider
import com.gigapingu.neon.core.model.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Default toot visibility levels, in the order shown in the picker. */
val PostVisibilityOptions = listOf("public", "unlisted", "private")

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val auth: AuthRepository,
    private val accounts: AccountRepository,
    private val pushEndpointProvider: PushEndpointProvider,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.Dark)

    val notificationsEnabled: StateFlow<Boolean> = settings.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val notificationAlertPrefs: StateFlow<NotificationAlertPrefs> = settings.notificationAlertPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationAlertPrefs())

    val notificationPermissionRequested: StateFlow<Boolean> = settings.notificationPermissionRequested
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val dynamicColorEnabled: StateFlow<Boolean> = settings.dynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val textScale: StateFlow<TextScale> = settings.textScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TextScale.Default)

    val iconScale: StateFlow<IconScale> = settings.iconScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IconScale.Default)

    private val _pushDistributorStatus = MutableStateFlow(pushEndpointProvider.getDistributorStatus())
    val pushDistributorStatus: StateFlow<PushDistributorStatus> = _pushDistributorStatus.asStateFlow()

    val me: StateFlow<Account?> = auth.me
    val instance: String? get() = auth.instance

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setNotificationsEnabled(enabled) }
    }

    fun setNotificationAlertPrefs(prefs: NotificationAlertPrefs) {
        viewModelScope.launch { settings.setNotificationAlertPrefs(prefs) }
    }

    fun markNotificationPermissionRequested() {
        viewModelScope.launch { settings.setNotificationPermissionRequested(true) }
    }

    fun refreshPushDistributorStatus() {
        _pushDistributorStatus.value = pushEndpointProvider.getDistributorStatus()
    }

    fun selectPushDistributor(packageName: String) {
        pushEndpointProvider.selectDistributor(packageName)
        refreshPushDistributorStatus()
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setDynamicColorEnabled(enabled) }
    }

    fun setTextScale(scale: TextScale) {
        viewModelScope.launch { settings.setTextScale(scale) }
    }

    fun setIconScale(scale: IconScale) {
        viewModelScope.launch { settings.setIconScale(scale) }
    }

    fun setDefaultPrivacy(visibility: String) {
        viewModelScope.launch {
            try {
                val updated = accounts.updateCredentials(defaultPrivacy = visibility)
                auth.updateMe(updated)
            } catch (e: Exception) {
                _errors.tryEmit(e.message ?: "Could not update default visibility")
            }
        }
    }

    fun setDefaultLanguage(languageCode: String) {
        viewModelScope.launch {
            try {
                val updated = accounts.updateCredentials(defaultLanguage = languageCode)
                auth.updateMe(updated)
            } catch (e: Exception) {
                _errors.tryEmit(e.message ?: "Could not update default language")
            }
        }
    }

    fun logout() {
        viewModelScope.launch { auth.logout() }
    }
}
