package com.gigapingu.neon.core.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.model.PreviewCard

@Composable
fun LinkPreviewCard(
    card: PreviewCard,
    modifier: Modifier = Modifier,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val uriHandler = LocalUriHandler.current
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.borderStrong, shape)
            .clickable {
                if (card.url.isNotEmpty()) {
                    runCatching { uriHandler.openUri(card.url) }
                }
            }
    ) {
        if (!card.image.isNullOrEmpty()) {
            AsyncImage(
                model = card.image,
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
        }
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = card.title,
                style = type.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (card.description.isNotEmpty()) {
                Text(
                    text = card.description,
                    style = type.bodySmall,
                    color = palette.textDim,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val domain = remember(card.url) {
                runCatching { java.net.URI(card.url).host }.getOrNull()?.removePrefix("www.") ?: card.url
            }
            if (domain.isNotEmpty()) {
                Text(
                    text = domain,
                    style = type.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = palette.textMute
                )
            }
        }
    }
}
