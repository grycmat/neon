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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.model.MediaAttachment
import com.gigapingu.neon.core.ui.Navigator

/** 1–4 media attachments in a rounded grid. Videos/gifs get a play badge. */
@Composable
fun MediaGrid(
    attachments: List<MediaAttachment>,
    modifier: Modifier = Modifier,
    sensitive: Boolean = false,
    onClick: ((MediaAttachment) -> Unit)? = null,
) {
    if (attachments.isEmpty()) return
    val items = attachments.take(4)
    val gap = 3.dp

    var revealed by rememberSaveable(inputs = arrayOf(attachments.firstOrNull()?.id ?: "")) {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
    ) {
        val contentModifier = if (sensitive && !revealed) {
            Modifier.blur(25.dp)
        } else {
            Modifier
        }

        Box(modifier = contentModifier) {
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

        if (sensitive && !revealed) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { revealed = true },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = "Sensitive content",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = "Sensitive Content",
                        style = NeonTheme.type.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Text(
                        text = "Tap to show",
                        style = NeonTheme.type.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Tile(attachment: MediaAttachment, onClick: ((MediaAttachment) -> Unit)?) {
    val palette = NeonTheme.palette
    val clickModifier = if (onClick != null) {
        Modifier.clickable { onClick(attachment) }
    } else {
        Modifier.clickable {
            Navigator.openMediaPreview(attachment.url, attachment.preview.ifEmpty { null })
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
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
