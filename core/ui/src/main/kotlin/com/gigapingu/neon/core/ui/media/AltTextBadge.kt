package com.gigapingu.neon.core.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.designsystem.theme.NeonTheme

/** Small "ALT" pill shown over media that has alt text; tap reveals it via [AltTextSheet]. */
@Composable
fun AltTextBadge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = "ALT",
            style = NeonTheme.type.labelSmall,
            color = Color.White,
        )
    }
}

/** Bottom sheet showing an attachment's full alt text. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AltTextSheet(altText: String, onDismiss: () -> Unit) {
    val palette = NeonTheme.palette
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceSolid,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Text(
                "Image description",
                style = NeonTheme.type.titleMedium,
                color = palette.text,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(altText, style = NeonTheme.type.bodyMedium, color = palette.text)
        }
    }
}
