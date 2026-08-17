package com.gigapingu.neon.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * foss ships with no Play Core dependency at all (F-Droid inclusion policy) — updates come from
 * F-Droid/IzzyOnDroid/Obtainium instead, so the check is a silent no-op.
 */
@Singleton
class NoOpAppUpdateController @Inject constructor() : AppUpdateController {
    private val _state = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Idle)
    override val state: StateFlow<AppUpdateUiState> = _state.asStateFlow()

    override suspend fun checkAndStart(launcher: ActivityResultLauncher<IntentSenderRequest>) = Unit
    override fun onFlowResult(resultCode: Int) = Unit
    override fun completeUpdate() = Unit
    override fun dismissInstallPrompt() = Unit
}
