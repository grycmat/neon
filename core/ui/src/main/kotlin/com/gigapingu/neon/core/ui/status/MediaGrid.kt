package com.gigapingu.neon.core.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gigapingu.neon.core.designsystem.component.neonSharedElement
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.model.MediaAttachment
import com.gigapingu.neon.core.ui.LocalNeonNavigator


/** 1–4 media attachments in a rounded grid. Videos/gifs get a play badge. */
@Composable
fun MediaGrid(
    attachments: List<MediaAttachment>,
    modifier: Modifier = Modifier,
    onClick: ((MediaAttachment) -> Unit)? = null,
) {
    if (attachments.isEmpty()) return
    val items = attachments.take(4)
    val gap = 3.dp
    Box(
        modifier = modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
    ) {
        when (items.size) {
            1 -> Box(Modifier.aspectRatio(16f / 10f)) { Tile(items[0], onClick) }
            2 -> Row(Modifier.aspectRatio(16f / 9f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                Box(Modifier.weight(1f).fillMaxSize()) { Tile(items[0], onClick) }
                Box(Modifier.weight(1f).fillMaxSize()) { Tile(items[1], onClick) }
            }
            else -> Row(Modifier.aspectRatio(16f / 9f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                Box(Modifier.weight(1f).fillMaxSize()) { Tile(items[0], onClick) }
                Column(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Box(Modifier.weight(1f).fillMaxWidth()) { Tile(items[1], onClick) }
                    if (items.size == 3) {
                        Box(Modifier.weight(1f).fillMaxWidth()) { Tile(items[2], onClick) }
                    } else {
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            Box(Modifier.weight(1f).fillMaxSize()) { Tile(items[2], onClick) }
                            Box(Modifier.weight(1f).fillMaxSize()) { Tile(items[3], onClick) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(attachment: MediaAttachment, onClick: ((MediaAttachment) -> Unit)?) {
    val palette = NeonTheme.palette
    val navigator = LocalNeonNavigator.current
    val clickModifier = if (onClick != null) {
        Modifier.clickable { onClick(attachment) }
    } else {
        Modifier.clickable {
            navigator.openMediaPreview(attachment.url, attachment.preview.ifEmpty { null })
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Hero: this tile morphs into the full-screen viewer.
            .neonSharedElement("media-${attachment.url}")
            .background(palette.gradientSoft)
            .then(clickModifier),
    ) {
        if (attachment.preview.isNotEmpty()) {
            AsyncImage(
                model = attachment.preview,
                contentDescription = attachment.altText.ifEmpty { null },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (attachment.isPlayable) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = .45f))
                    .border(1.dp, Color.White.copy(alpha = .6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
