package com.gigapingu.neon.core.ui

import android.content.Intent
import android.net.Uri
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
            Text(
                message,
                color = palette.textDim,
                style = type.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=io.heckel.ntfy"))
                        )
                    } catch (e: Exception) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://ntfy.sh"))
                        )
                    }
                }
            ) {
                Text("Get ntfy", color = palette.cyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        onDismiss()
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://unifiedpush.org"))
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
