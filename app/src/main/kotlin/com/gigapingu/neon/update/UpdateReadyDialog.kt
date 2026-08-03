package com.gigapingu.neon.update

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.designsystem.theme.NeonTheme

/**
 * Shown once a flexible in-app update has finished downloading in the background.
 * "Later" keeps the downloaded update on disk — it is re-offered on the next resume.
 */
@Composable
fun UpdateReadyDialog(
    onRestart: () -> Unit,
    onLater: () -> Unit,
) {
    val palette = NeonTheme.palette
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("Update ready", color = palette.text) },
        text = {
            Text(
                "A new version of Neon has been downloaded. Restart the app to finish installing.",
                color = palette.textDim,
            )
        },
        confirmButton = {
            TextButton(onClick = onRestart) {
                Text("Restart", color = palette.pink)
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text("Later", color = palette.textMute)
            }
        },
        containerColor = palette.surfaceSolid,
        shape = RoundedCornerShape(20.dp),
    )
}

@Preview
@Composable
private fun UpdateReadyDialogPreview() {
    NeonTheme {
        UpdateReadyDialog(onRestart = {}, onLater = {})
    }
}
