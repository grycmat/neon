package com.gigapingu.neon.core.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.HtmlText
import com.gigapingu.neon.core.designsystem.component.NeonAvatar
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.relativeTime
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.ui.LocalNeonNavigator
import com.gigapingu.neon.core.ui.LocalStatusActionHandler

/**
 * The toot card. Unwraps boosts (with a boost header), renders CW spoilers,
 * body, quote, media, poll and the action row.
 */
@Composable
fun StatusCard(
    status: Status,
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
    navigateOnTap: Boolean = true,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val navigator = LocalNeonNavigator.current
    val display = status.display

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        onClick = if (navigateOnTap) {
            { navigator.openThread(display.id) }
        } else {
            null
        },
    ) {
        Column {
            if (status.isBoost) {
                Row(
                    modifier = Modifier.padding(bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Repeat,
                        contentDescription = "Boosted",
                        tint = palette.cyan,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "${status.account.displayNameOrUsername} boosted",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = type.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.cyan,
                    )
                }
            }
            Row {
                NeonAvatar(
                    account = display.account,
                    size = 38.dp,
                    modifier = Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                    ) { navigator.openProfile(display.account.id) },
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = null,
                                indication = null,
                            ) { navigator.openProfile(display.account.id) },
                    ) {
                        Text(
                            display.account.displayNameOrUsername,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = type.titleSmall,
                            color = palette.text,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            display.account.fullHandle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = type.bodySmall,
                            color = palette.textDim,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            relativeTime(display.createdAt),
                            style = type.bodySmall,
                            color = palette.textMute,
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    StatusBody(status = display)
                    display.quote?.let { quoted ->
                        QuoteCard(status = quoted, onClick = { navigator.openThread(quoted.id) })
                    }
                    if (display.mediaAttachments.isNotEmpty()) {
                        MediaGrid(attachments = display.mediaAttachments)
                    }
                    display.poll?.let { PollView(poll = it) }
                    if (showActions) {
                        StatusActions(status = display)
                    }
                }
            }
        }
    }
}

/** Body text with content-warning gating. */
@Composable
fun StatusBody(
    status: Status,
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val navigator = LocalNeonNavigator.current
    val actions = LocalStatusActionHandler.current
    var revealed by rememberSaveable(status.id) { androidx.compose.runtime.mutableStateOf(false) }
    val hasCw = status.spoilerText.isNotEmpty()
    val style = textStyle ?: type.bodyMedium

    Column(modifier = modifier) {
        if (hasCw) {
            val shape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
                    .background(palette.purple.copy(alpha = .12f), shape)
                    .border(1.dp, palette.purple.copy(alpha = .35f), shape)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                    ) { revealed = !revealed }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.WarningAmber,
                    contentDescription = "Content warning",
                    tint = palette.purple,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    status.spoilerText,
                    style = type.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (revealed) "Hide" else "Show",
                    style = type.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = palette.cyan,
                )
            }
        }
        if (!hasCw || revealed) {
            HtmlText(
                html = status.content,
                style = style,
                onHashtagClick = { tag -> navigator.openHashtag(tag) },
                onMentionClick = { acctOrUrl -> actions.openMention(status, acctOrUrl) },
            )
        }
    }
}

/** Larger body used by the thread's focused toot. */
val FocusedBodyFontSize = 16.sp
