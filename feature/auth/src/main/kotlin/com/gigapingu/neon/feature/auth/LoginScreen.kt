package com.gigapingu.neon.feature.auth

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassField
import com.gigapingu.neon.core.designsystem.component.GradientButton
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.component.NeonLabel
import com.gigapingu.neon.core.designsystem.theme.NeonTheme

/**
 * Login: instance URL entry + gradient hero, mirroring the design's start
 * screen typography. When the ViewModel produces an authorize URL, the
 * in-app OAuth WebView takes over.
 */
@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    uiState.authorizeUrl?.let { url ->
        OAuthWebViewScreen(
            authorizeUrl = url,
            redirectUri = viewModel.redirectUri,
            onCode = viewModel::onAuthCode,
            onDismiss = viewModel::onOAuthDismissed,
        )
        return
    }

    LoginContent(
        instanceHint = viewModel.defaultInstanceHint,
        busy = uiState.busy,
        error = uiState.error,
        onConnect = viewModel::connect,
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun LoginContent(
    instanceHint: String,
    busy: Boolean,
    error: String?,
    onConnect: (String) -> Unit,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    var instance by rememberSaveable { mutableStateOf("") }

    NeonBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 30.dp, top = 26.dp, end = 30.dp, bottom = 34.dp),
        ) {
            NeonLabel("Neon · Fediverse")
            Spacer(Modifier.weight(1f))
            Text(
                "A timeline that\nfeels like ",
                style = type.displayLarge.copy(fontSize = 42.sp),
                color = palette.text,
            )
            Text(
                "yours.",
                style = type.displayLarge.merge(
                    TextStyle(brush = palette.gradient, fontSize = 42.sp),
                ),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Sign in with any Mastodon instance. Your data stays on " +
                    "your server — Neon is just a nicer window into it.",
                style = type.bodyLarge,
                color = palette.textDim,
            )
            Spacer(Modifier.weight(1f))
            GlassField(label = "Instance") {
                BasicTextField(
                    value = instance,
                    onValueChange = { instance = it },
                    singleLine = true,
                    textStyle = type.bodyLarge.copy(
                        color = palette.text,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    cursorBrush = SolidColor(palette.cyan),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                        autoCorrectEnabled = false,
                    ),
                    keyboardActions = KeyboardActions(onGo = { onConnect(instance) }),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (instance.isEmpty()) {
                            Text(
                                instanceHint,
                                style = type.bodyLarge,
                                color = palette.textMute,
                            )
                        }
                        innerTextField()
                    },
                )
            }
            error?.let { message ->
                Log.d("Login error", message)
                Spacer(Modifier.height(12.dp))
                Text(message, style = type.bodySmall, color = palette.pink)
            }
            Spacer(Modifier.height(16.dp))
            GradientButton(
                label = "Connect",
                trailingArrow = true,
                busy = busy,
                onClick = { onConnect(instance) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Login", showBackground = true, heightDp = 760)
@Composable
private fun LoginPreview() {
    NeonTheme(darkTheme = true) {
        LoginContent(
            instanceHint = "mastodon.social",
            busy = false,
            error = null,
            onConnect = {},
        )
    }
}

@Preview(name = "Login — error", showBackground = true, heightDp = 760)
@Composable
private fun LoginErrorPreview() {
    NeonTheme(darkTheme = true) {
        LoginContent(
            instanceHint = "mastodon.social",
            busy = false,
            error = "Could not reach that instance — check the address.",
            onConnect = {},
        )
    }
}
