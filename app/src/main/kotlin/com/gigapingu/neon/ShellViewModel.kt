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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.Dark)

    init {
        viewModelScope.launch { auth.restore() }
    }
}
