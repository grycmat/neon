package com.gigapingu.neon.core.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.compactCount
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.ui.LocalNeonNavigator
import com.gigapingu.neon.core.ui.LocalStatusActionHandler

/**
 * Reply / boost / favourite / share row under every status.
 * Boost opens a sheet offering Boost and Quote (native quote API).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusActions(status: Status, modifier: Modifier = Modifier) {
    val palette = NeonTheme.palette
    val navigator = LocalNeonNavigator.current
    val actions = LocalStatusActionHandler.current
    var showBoostSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(top = 12.dp)
            .fillMaxWidth(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider),
        )
        Row(
            modifier = Modifier.padding(top = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionItem(
                icon = Icons.Outlined.ModeComment,
                count = status.repliesCount,
                onClick = { navigator.openCompose(replyToId = status.id) },
            )
            ActionItem(
                icon = Icons.Rounded.Repeat,
                count = status.reblogsCount,
                active = status.reblogged,
                activeColor = palette.cyan,
                onClick = { showBoostSheet = true },
            )
            ActionItem(
                icon = if (status.favourited) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                count = status.favouritesCount,
                active = status.favourited,
                activeColor = palette.pink,
                onClick = { actions.toggleFavourite(status) },
            )
            ActionItem(
                icon = Icons.Rounded.IosShare,
                onClick = { actions.share(status) },
            )
        }
    }

    if (showBoostSheet) {
        BoostSheet(
            status = status,
            onDismiss = { showBoostSheet = false },
            onBoost = {
                showBoostSheet = false
                actions.toggleBoost(status)
            },
            onQuote = {
                showBoostSheet = false
                navigator.openCompose(quotingId = status.id)
            },
        )
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    count: Int? = null,
    active: Boolean = false,
    activeColor: Color? = null,
    onClick: () -> Unit,
) {
    val palette = NeonTheme.palette
    val color = if (active) (activeColor ?: palette.cyan) else palette.textDim
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            // Comfortable ≥44dp hit target.
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        if (count != null && count > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                compactCount(count),
                style = NeonTheme.type.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = color,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoostSheet(
    status: Status,
    onDismiss: () -> Unit,
    onBoost: () -> Unit,
    onQuote: () -> Unit,
) {
    val palette = NeonTheme.palette
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceSolid,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SheetOption(
                icon = Icons.Rounded.Repeat,
                color = palette.cyan,
                title = if (status.reblogged) "Undo boost" else "Boost",
                subtitle = if (status.reblogged) {
                    "Remove from your followers' timelines"
                } else {
                    "Share with your followers"
                },
                onClick = onBoost,
            )
            SheetOption(
                icon = Icons.Rounded.FormatQuote,
                color = palette.purple,
                title = "Quote",
                subtitle = "Add your own commentary",
                onClick = onQuote,
            )
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cancel", style = NeonTheme.type.labelLarge, color = palette.textDim)
            }
        }
    }
}

@Composable
private fun SheetOption(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val palette = NeonTheme.palette
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface, shape)
            .border(1.dp, palette.border, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = .12f), RoundedCornerShape(13.dp))
                .border(1.dp, color.copy(alpha = .3f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column {
            Text(title, style = NeonTheme.type.titleSmall, color = palette.text)
            Text(subtitle, style = NeonTheme.type.bodySmall, color = palette.textDim)
        }
    }
}
