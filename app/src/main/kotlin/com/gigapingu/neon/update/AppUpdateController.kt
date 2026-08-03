package com.gigapingu.neon.update

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.gigapingu.neon.core.data.SettingsRepository
import com.gigapingu.neon.core.data.di.ApplicationScope
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

const val NEON_UPDATE_TAG = "NeonUpdate"

/** Play reports 0-5; 4 and up is "important enough to interrupt the user". */
private const val IMMEDIATE_PRIORITY = 4

/** Users who keep postponing a flexible update get the blocking flow after this long. */
private const val IMMEDIATE_STALENESS_DAYS = 14

private const val PLAY_STORE_PACKAGE = "com.android.vending"

/** What the shell needs to render; deliberately free of Play types so Compose sees stable state. */
sealed interface AppUpdateUiState {
    data object Idle : AppUpdateUiState
    data object Downloading : AppUpdateUiState
    data object ReadyToInstall : AppUpdateUiState
}

/**
 * Google Play in-app updates.
 *
 * Routine releases download in the background ([AppUpdateType.FLEXIBLE]) and then ask for a
 * restart; a release published with a high `inAppUpdatePriority`, or one the user has ignored for
 * [IMMEDIATE_STALENESS_DAYS], escalates to Play's blocking [AppUpdateType.IMMEDIATE] flow.
 *
 * Thin coroutine wrapper over a Play-services Task API, same shape as `FcmTokenProvider`.
 */
@Singleton
class AppUpdateController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val manager by lazy { AppUpdateManagerFactory.create(context) }

    private val _state = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Idle)
    val state: StateFlow<AppUpdateUiState> = _state.asStateFlow()

    /**
     * The `availableVersionCode` of an in-flight *flexible* flow, so cancelling it can be recorded
     * as a dismissal. Left null for immediate flows — those are meant to re-prompt.
     */
    private var pendingFlexibleVersion: Int? = null

    /** Only offer a given flexible update once per process, however often we re-check on resume. */
    private var promptedThisProcess = false

    // Anonymous object rather than a lambda so it can unregister itself.
    private val listener = object : InstallStateUpdatedListener {
        override fun onStateUpdate(installState: InstallState) {
            when (installState.installStatus()) {
                InstallStatus.DOWNLOADING -> _state.value = AppUpdateUiState.Downloading
                InstallStatus.DOWNLOADED -> {
                    _state.value = AppUpdateUiState.ReadyToInstall
                    manager.unregisterListener(this)
                }
                InstallStatus.INSTALLED, InstallStatus.CANCELED, InstallStatus.FAILED -> {
                    _state.value = AppUpdateUiState.Idle
                    manager.unregisterListener(this)
                }
                else -> Unit
            }
        }
    }

    /**
     * The single entry point — safe (and intended) to call on cold start and on every resume.
     * Play requires the resume call: it is what re-enters a stalled immediate update and what
     * surfaces a flexible download that finished while the app was away.
     */
    suspend fun checkAndStart(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (!installedFromPlay()) return

        val info = try {
            manager.requestAppUpdateInfo()
        } catch (e: Exception) {
            // Throws when the app isn't owned by the account, Play is unavailable, etc. Never fatal.
            Log.w(NEON_UPDATE_TAG, "Update check failed", e)
            return
        }

        // An immediate update was already running and got interrupted — resume it before anything else.
        if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            start(info, AppUpdateType.IMMEDIATE, launcher)
            return
        }

        // A flexible download may have completed (or still be running) across a process death.
        when (info.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                _state.value = AppUpdateUiState.ReadyToInstall
                return
            }
            InstallStatus.DOWNLOADING -> {
                _state.value = AppUpdateUiState.Downloading
                manager.registerListener(listener)
                return
            }
            else -> Unit
        }

        if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
            _state.value = AppUpdateUiState.Idle
            return
        }

        val urgent = info.updatePriority() >= IMMEDIATE_PRIORITY ||
            (info.clientVersionStalenessDays() ?: 0) >= IMMEDIATE_STALENESS_DAYS

        if (urgent && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            pendingFlexibleVersion = null
            start(info, AppUpdateType.IMMEDIATE, launcher)
            return
        }

        if (!info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) return
        if (promptedThisProcess) return

        val available = info.availableVersionCode()
        if (settings.dismissedUpdateVersion.first() == available) return

        pendingFlexibleVersion = available
        promptedThisProcess = true
        manager.registerListener(listener)
        start(info, AppUpdateType.FLEXIBLE, launcher)
    }

    private fun start(
        info: AppUpdateInfo,
        type: Int,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ) {
        try {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
        } catch (e: Exception) {
            Log.w(NEON_UPDATE_TAG, "Could not start update flow", e)
        }
    }

    /** Result of Play's consent UI. Cancelling a flexible offer suppresses it for that version. */
    fun onFlowResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) return

        pendingFlexibleVersion?.let { version ->
            scope.launch { settings.setDismissedUpdateVersion(version) }
        }
        pendingFlexibleVersion = null
        _state.value = AppUpdateUiState.Idle
        manager.unregisterListener(listener)
    }

    /** Restarts the app into the downloaded update. */
    fun completeUpdate() {
        manager.completeUpdate()
    }

    /**
     * "Later" on the restart prompt. The download stays on disk, so the next [checkAndStart]
     * sees `DOWNLOADED` and offers it again.
     */
    fun dismissInstallPrompt() {
        _state.value = AppUpdateUiState.Idle
    }

    /**
     * In-app updates only work for Play-installed builds. Gating on the installer rather than
     * `BuildConfig.DEBUG` keeps internal-app-sharing builds testable, and keeps sideloaded and
     * developer builds from logging a failed check on every resume.
     */
    private fun installedFromPlay(): Boolean = runCatching {
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
        installer == PLAY_STORE_PACKAGE
    }.getOrDefault(false) // PackageManager can throw on odd OEM builds; treat as "not from Play".
}
