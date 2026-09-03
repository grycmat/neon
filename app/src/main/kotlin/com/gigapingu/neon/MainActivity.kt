package com.gigapingu.neon

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.data.AuthStatus
import com.gigapingu.neon.core.data.ThemeMode
import com.gigapingu.neon.core.data.multiplier
import com.gigapingu.neon.core.data.push.PushDistributorStatus
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.ui.InstallDistributorDialog
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.update.AppUpdateUiState
import com.gigapingu.neon.update.UpdateReadyDialog
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle.Event
import android.os.Build
import androidx.compose.runtime.setValue
import android.Manifest
import android.content.pm.PackageManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: ShellViewModel by viewModels()

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.markNotificationPermissionRequested()
        onPermissionResult?.invoke(isGranted)
    }

    private val appUpdateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onUpdateFlowResult(result.resultCode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleNotificationIntent(intent)
        handleShareIntent(intent)

        setContent {
            val authStatus by viewModel.authStatus.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColorEnabled by viewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
            val textScale by viewModel.textScale.collectAsStateWithLifecycle()
            val iconScale by viewModel.iconScale.collectAsStateWithLifecycle()
            val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
            val notificationAlertPrefs by viewModel.notificationAlertPrefs.collectAsStateWithLifecycle()
            val updateState by viewModel.updateState.collectAsStateWithLifecycle()
            val pushDistributorStatus by viewModel.pushDistributorStatus.collectAsStateWithLifecycle()
            val pushProviderPromptShown by viewModel.pushProviderPromptShown.collectAsStateWithLifecycle()

            var hasNotificationPermission by remember {
                mutableStateOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                )
            }

            DisposableEffect(Unit) {
                onPermissionResult = { isGranted ->
                    hasNotificationPermission = isGranted
                }
                onDispose {
                    onPermissionResult = null
                }
            }

            var isAppForeground by remember { mutableStateOf(true) }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Event.ON_RESUME -> {
                            isAppForeground = true
                            viewModel.refreshPushDistributorStatus()
                            hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                            } else {
                                true
                            }
                        }

                        Event.ON_PAUSE -> {
                            isAppForeground = false
                        }

                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            splash.setKeepOnScreenCondition { authStatus == AuthStatus.Unknown }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    hasNotificationPermission = granted
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            LaunchedEffect(authStatus) {
                if (authStatus == AuthStatus.Authenticated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    hasNotificationPermission = granted
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            LaunchedEffect(authStatus, notificationsEnabled, hasNotificationPermission, notificationAlertPrefs) {
                viewModel.syncPushRegistration(hasNotificationPermission)
            }

            LaunchedEffect(isAppForeground) {
                viewModel.setStreamingForeground(isAppForeground)
            }

            // Fires on cold start and again on every resume (isAppForeground is flipped by the
            // lifecycle observer above) — Play needs the resume check to re-enter a stalled
            // immediate update and to surface a flexible download that finished in the background.
            LaunchedEffect(isAppForeground) {
                if (isAppForeground) viewModel.checkForUpdate(appUpdateLauncher)
            }

            val darkTheme = when (themeMode) {
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
                ThemeMode.System -> isSystemInDarkTheme()
            }
            NeonTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColorEnabled,
                textScale = textScale.multiplier,
                iconScale = iconScale.multiplier,
            ) {
                NeonApp(viewModel = viewModel, modifier = Modifier)

                // Its own window, so it floats above the shell and any pushed Nav3 screen.
                if (authStatus != AuthStatus.Unknown && updateState is AppUpdateUiState.ReadyToInstall) {
                    UpdateReadyDialog(
                        onRestart = viewModel::completeUpdate,
                        onLater = viewModel::dismissUpdatePrompt,
                    )
                }

                // First run after login: prompt once if no UnifiedPush distributor is installed
                if (authStatus == AuthStatus.Authenticated &&
                    pushDistributorStatus is PushDistributorStatus.NotInstalled &&
                    !pushProviderPromptShown
                ) {
                    InstallDistributorDialog(
                        isFirstRunPrompt = true,
                        onDismiss = viewModel::dismissPushProviderPrompt,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val statusId = intent.getStringExtra("status_id")
        val openNotifications = intent.getBooleanExtra("open_notifications", false)
        if (statusId != null || openNotifications) {
            Navigator.handleNotificationClick(statusId = statusId, openNotificationsTab = openNotifications)
        }
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        val text = if (intent.action == Intent.ACTION_SEND) {
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        } else {
            null
        }
        val uris: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND ->
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.let { listOf(it) }.orEmpty()

            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()

            else -> emptyList()
        }
        if (text != null || uris.isNotEmpty()) {
            Navigator.handleShare(text = text, mediaUris = uris.map { it.toString() })
        }
    }
}
