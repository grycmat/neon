package com.gigapingu.neon

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.data.AuthStatus
import com.gigapingu.neon.core.data.ThemeMode
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.ui.Navigator
import dagger.hilt.android.AndroidEntryPoint
import org.unifiedpush.android.connector.UnifiedPush
import androidx.compose.runtime.setValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        handleNotificationIntent(intent)

        setContent {
            val viewModel: ShellViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val authStatus by viewModel.authStatus.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()

            var hasNotificationPermission by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                )
            }

            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        hasNotificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            splash.setKeepOnScreenCondition { authStatus == AuthStatus.Unknown }

            LaunchedEffect(authStatus, notificationsEnabled, hasNotificationPermission) {
                if (authStatus == AuthStatus.Authenticated && notificationsEnabled && hasNotificationPermission) {
                    UnifiedPush.tryUseCurrentOrDefaultDistributor(this@MainActivity) { success ->
                        if (success) {
                            UnifiedPush.register(this@MainActivity)
                        }
                    }
                } else if (authStatus == AuthStatus.Authenticated) {
                    runCatching { UnifiedPush.unregister(this@MainActivity) }
                }
            }

            val darkTheme = when (themeMode) {
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
                ThemeMode.System -> isSystemInDarkTheme()
            }
            NeonTheme(darkTheme = darkTheme) {
                NeonApp(viewModel = viewModel, modifier = Modifier)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val statusId = intent.getStringExtra("status_id")
        val openNotifications = intent.getBooleanExtra("open_notifications", false)
        if (statusId != null || openNotifications) {
            Navigator.handleNotificationClick(statusId = statusId, openNotificationsTab = openNotifications)
        }
    }
}
