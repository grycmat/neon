package com.gigapingu.neon.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gigapingu.neon.core.designsystem.theme.NeonTheme

/** A single segmented-filter pill, e.g. Timeline's Home/Local/Federated or Notifications' type filter. */
@Composable
fun SegmentPill(label: String, active: Boolean, onClick: () -> Unit) {
    val palette = NeonTheme.palette
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(shape)
            .background(if (active) palette.surfaceHi else Color.Transparent)
            .border(1.dp, if (active) palette.borderStrong else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = NeonTheme.type.labelLarge.copy(fontSize = 14.sp),
            color = if (active) palette.text else palette.textDim,
        )
    }
}
