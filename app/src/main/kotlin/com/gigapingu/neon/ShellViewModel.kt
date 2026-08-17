package com.gigapingu.neon

import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.data.AuthStatus
import com.gigapingu.neon.core.data.BigScreenLayout
import com.gigapingu.neon.core.data.SettingsRepository
import com.gigapingu.neon.core.data.StreamingRepository
import com.gigapingu.neon.core.data.ThemeMode
import com.gigapingu.neon.core.data.TimelineKind
import com.gigapingu.neon.core.data.TimelineRepository
import com.gigapingu.neon.core.data.push.NotificationAlertPrefs
import com.gigapingu.neon.core.data.push.PushDistributorStatus
import com.gigapingu.neon.core.data.push.PushEndpointProvider
import com.gigapingu.neon.core.data.push.PushRepository
import com.gigapingu.neon.core.model.Account
import com.gigapingu.neon.feature.notifications.NEON_PUSH_TAG
import com.gigapingu.neon.update.AppUpdateController
import com.gigapingu.neon.update.AppUpdateUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-level state: auth gate and theme. */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val settings: SettingsRepository,
    private val timelines: TimelineRepository,
    private val pushRepository: PushRepository,
    private val pushEndpointProvider: PushEndpointProvider,
    private val streamingRepository: StreamingRepository,
    private val appUpdate: AppUpdateController,
) : ViewModel() {

    val authStatus: StateFlow<AuthStatus> = auth.status
    val me: StateFlow<Account?> = auth.me
    val instanceHost: String? get() = auth.instance

    private val _restoreError = MutableStateFlow<String?>(null)
    val restoreError: StateFlow<String?> = _restoreError.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.Dark)

    val notificationsEnabled: StateFlow<Boolean> = settings.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val notificationAlertPrefs: StateFlow<NotificationAlertPrefs> = settings.notificationAlertPrefs
        .stateIn(viewModelScope, SharingStarted.Eagerly, NotificationAlertPrefs())

    val bigScreenLayout: StateFlow<BigScreenLayout> = settings.bigScreenLayout
        .stateIn(viewModelScope, SharingStarted.Eagerly, BigScreenLayout.TwoPane)

    /** Cycles the top app bar's layout toggle: List -> TwoPane -> Grid -> List. */
    fun cycleBigScreenLayout() {
        val next = when (bigScreenLayout.value) {
            BigScreenLayout.List -> BigScreenLayout.TwoPane
            BigScreenLayout.TwoPane -> BigScreenLayout.Grid
            BigScreenLayout.Grid -> BigScreenLayout.List
        }
        viewModelScope.launch { settings.setBigScreenLayout(next) }
    }

    val dynamicColorEnabled: StateFlow<Boolean> = settings.dynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val pushProviderPromptShown: StateFlow<Boolean> = settings.pushProviderPromptShown
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _pushDistributorStatus = MutableStateFlow(pushEndpointProvider.getDistributorStatus())
    val pushDistributorStatus: StateFlow<PushDistributorStatus> = _pushDistributorStatus.asStateFlow()

    fun refreshPushDistributorStatus() {
        _pushDistributorStatus.value = pushEndpointProvider.getDistributorStatus()
    }

    fun dismissPushProviderPrompt() {
        viewModelScope.launch { settings.setPushProviderPromptShown(true) }
    }

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
     * Registers (or removes) the push subscription based on current auth, the notifications
     * setting, and OS notification permission. Transport is flavor-dependent — see
     * [PushEndpointProvider]. Safe to call on every relevant state change — [PushRepository]
     * skips redundant re-registration.
     */
    fun syncPushRegistration(hasNotificationPermission: Boolean) {
        viewModelScope.launch {
            if (authStatus.value != AuthStatus.Authenticated) return@launch
            try {
                if (notificationsEnabled.value && hasNotificationPermission) {
                    val endpoint = pushEndpointProvider.getEndpoint()
                    if (endpoint != null) {
                        pushRepository.register(endpoint, notificationAlertPrefs.value)
                        Log.i(NEON_PUSH_TAG, "Registered push subscription with instance")
                    } else {
                        Log.w(NEON_PUSH_TAG, "No push endpoint available yet — skipping registration")
                    }
                } else {
                    pushRepository.unregister()
                }
            } catch (e: Exception) {
                Log.e(NEON_PUSH_TAG, "Push registration sync failed", e)
            }
        }
    }

    /** Connects/disconnects the live streaming WebSocket based on app foreground state. */
    fun setStreamingForeground(active: Boolean) {
        streamingRepository.setForeground(active)
    }

    /** Play in-app update state; drives the "Update ready" restart prompt in [MainActivity]. */
    val updateState: StateFlow<AppUpdateUiState> = appUpdate.state

    /**
     * Checks Play for an update and starts the flow if one applies. Called on cold start and on
     * every resume — Play needs the resume call to re-enter a stalled immediate update.
     */
    suspend fun checkForUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        appUpdate.checkAndStart(launcher)
    }

    fun onUpdateFlowResult(resultCode: Int) = appUpdate.onFlowResult(resultCode)

    fun completeUpdate() = appUpdate.completeUpdate()

    fun dismissUpdatePrompt() = appUpdate.dismissInstallPrompt()

    /**
     * One-shot read of whether the POST_NOTIFICATIONS dialog has ever been shown —
     * reads the DataStore Flow directly rather than the eagerly-`stateIn`'d
     * [notificationsEnabled]-style StateFlow, since at cold-start time that would
     * still report its initial default before the real persisted value loads.
     */
    suspend fun hasRequestedNotificationPermission(): Boolean =
        settings.notificationPermissionRequested.first()

    fun markNotificationPermissionRequested() {
        viewModelScope.launch { settings.setNotificationPermissionRequested(true) }
    }
}
