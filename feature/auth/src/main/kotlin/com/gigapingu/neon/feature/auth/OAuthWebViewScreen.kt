package com.gigapingu.neon.feature.auth

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.theme.NeonTheme

/**
 * In-app WebView for /oauth/authorize. Intercepts the redirect URI and
 * hands the authorization code back via [onCode].
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OAuthWebViewScreen(
    authorizeUrl: String,
    redirectUri: String,
    onCode: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = NeonTheme.palette
    var loading by remember { mutableStateOf(true) }
    var handled by remember { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    NeonBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    onClick = onDismiss,
                    contentDescription = "Cancel login",
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    authorizeUrl.toUri().host.orEmpty(),
                    style = NeonTheme.type.headlineMedium,
                    color = palette.text,
                )
            }
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    if (url.startsWith(redirectUri) && !handled) {
                                        handled = true
                                        val code = url.toUri().getQueryParameter("code")
                                        if (code != null) onCode(code) else onDismiss()
                                        return true
                                    }
                                    return false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loading = false
                                }
                            }
                            loadUrl(authorizeUrl)
                        }
                    },
                )
                if (loading) {
                    CircularProgressIndicator(
                        color = palette.cyan,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}
