package com.gigapingu.neon.core.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gigapingu.neon.core.designsystem.component.EmojiText
import com.gigapingu.neon.core.designsystem.component.HtmlText
import com.gigapingu.neon.core.designsystem.component.NeonAvatar
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.relativeTime
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.StatusActionService

/** Embedded quoted status — compact glass card inside a StatusCard. */
@Composable
fun QuoteCard(
    status: Status,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .padding(top = 11.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 13.dp, top = 12.dp, end = 13.dp, bottom = 12.dp),
    ) {
        Row {
            NeonAvatar(account = status.account, size = 22.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                EmojiText(
                    status.account.displayNameOrUsername,
                    emojis = status.account.emojis,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = type.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = palette.text,
                )
                Text(
                    status.account.fullHandle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = type.bodySmall,
                    color = palette.textDim,
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                relativeTime(status.createdAt),
                style = type.bodySmall,
                color = palette.textMute,
            )
        }
        Spacer(Modifier.height(7.dp))
        HtmlText(
            status.content,
            maxLines = 5,
            style = type.bodyMedium.copy(fontSize = 13.5.sp),
            emojis = status.emojis,
            onHashtagClick = { tag -> Navigator.openHashtagSearch(tag) },
            onMentionClick = { acctOrUrl -> StatusActionService.openMention(status, acctOrUrl) },
            onLinkClick = { url -> StatusActionService.openUrl(url) },
        )
        if (status.mediaAttachments.isNotEmpty()) {
            MediaGrid(attachments = status.mediaAttachments, sensitive = status.sensitive)
        }
    }
}
