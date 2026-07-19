package com.gigapingu.neon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.data.AuthStatus
import com.gigapingu.neon.core.data.SettingsRepository
import com.gigapingu.neon.core.data.ThemeMode
import com.gigapingu.neon.core.model.Account
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
}
