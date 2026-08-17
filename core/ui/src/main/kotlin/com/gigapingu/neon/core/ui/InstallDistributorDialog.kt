package com.gigapingu.neon.core.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.designsystem.theme.NeonTheme

private const val NTFY_FDROID_URL = "https://f-droid.org/en/packages/io.heckel.ntfy/"
private const val NTFY_PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=io.heckel.ntfy"
private const val UNIFIED_PUSH_URL = "https://unifiedpush.org"

@Composable
fun InstallDistributorDialog(
    isFirstRunPrompt: Boolean = false,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val message = buildString {
        append("Push notifications on de-googled devices require an open-source UnifiedPush distributor such as ntfy.\n\nInstall ntfy and open it once to enable push notifications in Neon.")
        if (isFirstRunPrompt) {
            append("\n\nYou can do this later in settings.")
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Push Provider Required", color = palette.text, style = type.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    message,
                    color = palette.textDim,
                    style = type.bodyMedium,
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        onDismiss()
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(NTFY_FDROID_URL))
                        )
                    }
                ) {
                    Text("Get on F-Droid", color = palette.cyan, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = {
                        onDismiss()
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=io.heckel.ntfy"))
                            )
                        } catch (e: Exception) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(NTFY_PLAY_STORE_URL))
                            )
                        }
                    }
                ) {
                    Text("Get on Play Store", color = palette.cyan, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        onDismiss()
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(UNIFIED_PUSH_URL))
                        )
                    }
                ) {
                    Text("Learn more", color = palette.textDim)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = palette.textMute)
                }
            }
        },
        containerColor = palette.surfaceSolid,
        shape = RoundedCornerShape(20.dp),
    )
}

