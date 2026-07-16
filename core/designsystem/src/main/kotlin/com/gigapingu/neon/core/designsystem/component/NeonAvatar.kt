package com.gigapingu.neon.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gigapingu.neon.core.designsystem.theme.NeonAccents
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.model.Account

/**
 * Avatar with the accent-trio gradient fallback and optional glow ring.
 * A non-null [heroKey] lets it morph across screens as a shared element.
 */
@Composable
fun NeonAvatar(
    account: Account?,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    ring: Boolean = false,
    heroKey: String? = null,
) {
    val palette = NeonTheme.palette
    val url = account?.avatar.orEmpty()
    Box(
        modifier = modifier
            .then(if (heroKey != null) Modifier.neonSharedElement(heroKey) else Modifier)
            .size(size)
            .then(
                if (ring) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        ambientColor = NeonAccents.Purple.copy(alpha = .35f),
                        spotColor = NeonAccents.Purple.copy(alpha = .35f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(NeonAccents.Pink, NeonAccents.Purple)),
            )
            .border(
                width = if (ring) 2.dp else 1.dp,
                color = if (ring) Color.White.copy(alpha = .85f) else palette.border,
                shape = CircleShape,
            ),
    ) {
        if (url.isNotEmpty()) {
            AsyncImage(
                model = url,
                contentDescription = account?.displayNameOrUsername,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
