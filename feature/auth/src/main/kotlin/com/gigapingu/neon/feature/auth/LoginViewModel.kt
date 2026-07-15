package com.gigapingu.neon.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val busy: Boolean = false,
    val error: String? = null,
    /** Non-null → open the OAuth WebView at this URL. */
    val authorizeUrl: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val defaultInstanceHint: String get() = auth.defaultInstanceHint
    val redirectUri: String get() = auth.redirectUri

    fun connect(rawInstance: String) {
        val instance = rawInstance.trim().ifEmpty { auth.defaultInstanceHint }
        _uiState.value = LoginUiState(busy = true)
        viewModelScope.launch {
            try {
                val url = auth.beginLogin(instance)
                _uiState.value = LoginUiState(busy = true, authorizeUrl = url)
            } catch (e: Exception) {
                _uiState.value = LoginUiState(error = "Could not connect: ${e.message}")
            }
        }
    }

    /** The WebView intercepted the redirect and produced an auth code. */
    fun onAuthCode(code: String) {
        _uiState.value = LoginUiState(busy = true)
        viewModelScope.launch {
            try {
                auth.finishLogin(code)
                // The auth gate swaps to the shell.
            } catch (e: Exception) {
                _uiState.value = LoginUiState(error = "Could not connect: ${e.message}")
            }
        }
    }

    fun onOAuthDismissed() {
        _uiState.value = LoginUiState()
    }
}
