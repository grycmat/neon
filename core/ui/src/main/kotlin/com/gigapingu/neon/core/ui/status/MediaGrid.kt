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
import com.gigapingu.neon.core.model.MediaType
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.media.VideoPlayer
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.sp

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
    var muted by remember { mutableStateOf(true) }

    val clickModifier = if (attachment.isPlayable) {
        if (attachment.type == MediaType.Gifv) {
            if (onClick != null) {
                Modifier.clickable { onClick(attachment) }
            } else {
                Modifier.clickable {
                    Navigator.openMediaPreview(attachment.url, attachment.preview.ifEmpty { null }, attachment.rawType)
                }
            }
        } else {
            Modifier.clickable { muted = !muted }
        }
    } else {
        if (onClick != null) {
            Modifier.clickable { onClick(attachment) }
        } else {
            Modifier.clickable {
                Navigator.openMediaPreview(attachment.url, attachment.preview.ifEmpty { null }, attachment.rawType)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.gradientSoft)
            .then(clickModifier),
    ) {
        if (attachment.isPlayable) {
            VideoPlayer(
                url = attachment.url,
                modifier = Modifier.fillMaxSize(),
                muted = muted,
                looping = attachment.type == MediaType.Gifv,
                useController = false,
            )

            if (attachment.type == MediaType.Video) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (muted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                        contentDescription = if (muted) "Muted" else "Unmuted",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable {
                            if (onClick != null) {
                                onClick(attachment)
                            } else {
                                Navigator.openMediaPreview(
                                    url = attachment.url,
                                    previewUrl = attachment.preview.ifEmpty { null },
                                    type = attachment.rawType
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            if (attachment.preview.isNotEmpty()) {
                AsyncImage(
                    model = attachment.preview,
                    contentDescription = attachment.altText.ifEmpty { null },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
