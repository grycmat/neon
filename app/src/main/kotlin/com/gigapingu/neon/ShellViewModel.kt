package com.gigapingu.neon

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.data.AuthStatus
import com.gigapingu.neon.core.data.SettingsRepository
import com.gigapingu.neon.core.data.ThemeMode
import com.gigapingu.neon.core.data.TimelineKind
import com.gigapingu.neon.core.data.TimelineRepository
import com.gigapingu.neon.core.data.push.PushRepository
import com.gigapingu.neon.core.model.Account
import com.gigapingu.neon.feature.notifications.FcmTokenProvider
import com.gigapingu.neon.feature.notifications.NEON_PUSH_TAG
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-level state: auth gate and theme. */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val auth: AuthRepository,
    settings: SettingsRepository,
    private val timelines: TimelineRepository,
    private val pushRepository: PushRepository,
    private val fcmTokenProvider: FcmTokenProvider,
) : ViewModel() {

    val authStatus: StateFlow<AuthStatus> = auth.status
    val me: StateFlow<Account?> = auth.me

    private val _restoreError = MutableStateFlow<String?>(null)
    val restoreError: StateFlow<String?> = _restoreError.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.Dark)

    val notificationsEnabled: StateFlow<Boolean> = settings.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _selectedTab = MutableStateFlow<Int?>(null)
    val selectedTab: StateFlow<Int?> = _selectedTab.asStateFlow()

    fun selectTab(page: Int) {
        _selectedTab.value = page
    }

    fun clearSelectedTab() {
        _selectedTab.value = null
    }

    init {
        performRestore()

        viewModelScope.launch {
            authStatus.collect { status ->
                if (status == AuthStatus.Authenticated) {
                    TimelineKind.entries.forEach { kind ->
                        launch { timelines.load(kind) }
                    }
                }
            }
        }
    }

    fun performRestore() {
        viewModelScope.launch {
            _restoreError.value = null
            try {
                auth.restore()
            } catch (e: Exception) {
                _restoreError.value = e.message ?: "Could not restore auth status"
            }
        }
    }

    /**
     * Registers (or removes) the FCM/relay push subscription based on current auth,
     * the notifications setting, and OS notification permission. Safe to call on every
     * relevant state change — [PushRepository] skips redundant re-registration.
     */
    fun syncPushRegistration(hasNotificationPermission: Boolean) {
        viewModelScope.launch {
            if (authStatus.value != AuthStatus.Authenticated) return@launch
            try {
                if (notificationsEnabled.value && hasNotificationPermission) {
                    val token = fcmTokenProvider.getToken()
                    if (token != null) {
                        pushRepository.register(token)
                        Log.i(NEON_PUSH_TAG, "Registered push subscription with instance")
                    } else {
                        Log.w(NEON_PUSH_TAG, "No FCM token available — skipping push registration")
                    }
                } else {
                    pushRepository.unregister()
                }
            } catch (e: Exception) {
                Log.e(NEON_PUSH_TAG, "Push registration sync failed", e)
            }
        }
    }
}
