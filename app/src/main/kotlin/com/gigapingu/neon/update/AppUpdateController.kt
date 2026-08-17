package com.gigapingu.neon.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.flow.StateFlow

/** What the shell needs to render; deliberately free of Play types so Compose sees stable state. */
sealed interface AppUpdateUiState {
    data object Idle : AppUpdateUiState
    data object Downloading : AppUpdateUiState
    data object ReadyToInstall : AppUpdateUiState
}

/**
 * In-app update check, flavor-swapped: [PlayAppUpdateController] (gms) drives Google Play's
 * in-app update flow; [NoOpAppUpdateController] (foss) never has anything to offer, since F-Droid
 * builds have no Play Core dependency at all (F-Droid inclusion policy).
 */
interface AppUpdateController {
    val state: StateFlow<AppUpdateUiState>

    /**
     * The single entry point — safe (and intended) to call on cold start and on every resume.
     */
    suspend fun checkAndStart(launcher: ActivityResultLauncher<IntentSenderRequest>)

    /** Result of the update flow's consent UI, if any. */
    fun onFlowResult(resultCode: Int)

    /** Restarts the app into a downloaded update. */
    fun completeUpdate()

    /** "Later" on the restart prompt. */
    fun dismissInstallPrompt()
}
