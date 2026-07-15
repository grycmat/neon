package com.gigapingu.neon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.data.AuthStatus
import com.gigapingu.neon.core.data.ThemeMode
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: ShellViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val authStatus by viewModel.authStatus.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            splash.setKeepOnScreenCondition { authStatus == AuthStatus.Unknown }

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
}
