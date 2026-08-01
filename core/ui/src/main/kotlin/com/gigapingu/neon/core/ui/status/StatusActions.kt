package com.gigapingu.neon.core.ui.status

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.designsystem.theme.NeonMotion
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.compactCount
import com.gigapingu.neon.core.model.Account
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.ui.AccountRow
import com.gigapingu.neon.core.ui.ErrorPane
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.StatusActionService

/**
 * Reply / boost / favourite / share row under every status.
 * Boost opens a sheet offering Boost and Quote (native quote API).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusActions(status: Status, modifier: Modifier = Modifier) {
    val palette = NeonTheme.palette
    var showBoostSheet by remember { mutableStateOf(false) }
    var engagersKind by remember { mutableStateOf<EngagersKind?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    val isOwn = StatusActionService.isOwnStatus(status)

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
                contentDescription = "Reply",
                count = status.repliesCount,
                onClick = { Navigator.openCompose(replyToId = status.id) },
            )
            ActionItem(
                icon = Icons.Rounded.Repeat,
                contentDescription = if (status.reblogged) "Undo boost" else "Boost",
                count = status.reblogsCount,
                active = status.reblogged,
                activeColor = palette.cyan,
                spinOnActivate = true,
                onClick = { showBoostSheet = true },
                onLongClick = { engagersKind = EngagersKind.Boosts },
                onLongClickLabel = "See who boosted",
            )
            ActionItem(
                icon = if (status.favourited) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = if (status.favourited) "Unfavourite" else "Favourite",
                count = status.favouritesCount,
                active = status.favourited,
                activeColor = palette.pink,
                onClick = { StatusActionService.toggleFavourite(status) },
                onLongClick = { engagersKind = EngagersKind.Favourites },
                onLongClickLabel = "See who favourited",
            )
            ActionItem(
                icon = if (status.bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                contentDescription = if (status.bookmarked) "Remove bookmark" else "Bookmark",
                active = status.bookmarked,
                activeColor = palette.cyan,
                onClick = { StatusActionService.toggleBookmark(status) },
            )
            ActionItem(
                icon = Icons.Rounded.IosShare,
                contentDescription = "Share",
                onClick = { StatusActionService.share(status) },
            )
            if (isOwn) {
                ActionItem(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = "More options",
                    onClick = { showContextMenu = true },
                )
            }
        }
    }

    if (showContextMenu) {
        StatusContextMenuSheet(
            status = status,
            onDismiss = { showContextMenu = false },
            onRequestReport = {},
        )
    }

    if (showBoostSheet) {
        BoostSheet(
            status = status,
            onDismiss = { showBoostSheet = false },
            onBoost = {
                showBoostSheet = false
                StatusActionService.toggleBoost(status)
            },
            onQuote = {
                showBoostSheet = false
                Navigator.openCompose(quotingId = status.id)
            },
        )
    }

    engagersKind?.let { kind ->
        EngagersSheet(status = status, kind = kind, onDismiss = { engagersKind = null })
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    contentDescription: String,
    count: Int? = null,
    active: Boolean = false,
    activeColor: Color? = null,
    spinOnActivate: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    onClick: () -> Unit,
) {
    val palette = NeonTheme.palette
    val color by animateColorAsState(
        targetValue = if (active) (activeColor ?: palette.cyan) else palette.textDim,
        animationSpec = NeonMotion.quick(),
        label = "actionTint",
    )
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    // Pop only on a fresh activation, not when a card scrolls into view already active.
    var wasActive by remember { mutableStateOf(active) }
    LaunchedEffect(active) {
        if (active && !wasActive) {
            if (spinOnActivate) {
                rotation.snapTo(0f)
                rotation.animateTo(360f, NeonMotion.screen())
            }
            scale.animateTo(1.4f, spring(stiffness = Spring.StiffnessHigh))
            scale.animateTo(1f, NeonMotion.bouncy())
        }
        wasActive = active
    }
    val fullDescription = if (count != null && count > 0) {
        "$contentDescription, ${compactCount(count)}"
    } else {
        contentDescription
    }
    Row(
        modifier = Modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel,
            )
            // Comfortable ≥44dp hit target.
            .padding(vertical = 10.dp)
            .clearAndSetSemantics {
                this.contentDescription = fullDescription
                role = Role.Button
                if (onLongClick != null) {
                    this.onLongClick(label = onLongClickLabel) {
                        onLongClick()
                        true
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(17.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    rotationZ = rotation.value
                },
        )
        if (count != null && count > 0) {
            Spacer(Modifier.width(6.dp))
            // Counts roll like an odometer: up on increment, down on decrement.
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInVertically(NeonMotion.quick()) { dir * it } +
                        fadeIn(NeonMotion.quick())) togetherWith
                        (slideOutVertically(NeonMotion.quick()) { -dir * it } +
                            fadeOut(NeonMotion.quick()))
                },
                label = "actionCount",
            ) { value ->
                Text(
                    compactCount(value),
                    style = NeonTheme.type.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = color,
                )
            }
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

private enum class EngagersKind { Favourites, Boosts }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EngagersSheet(status: Status, kind: EngagersKind, onDismiss: () -> Unit) {
    val palette = NeonTheme.palette
    var accounts by remember { mutableStateOf<List<Account>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryToken) {
        error = null
        accounts = null
        val result = when (kind) {
            EngagersKind.Favourites -> StatusActionService.getFavouritedBy(status.id)
            EngagersKind.Boosts -> StatusActionService.getRebloggedBy(status.id)
        }
        result.onSuccess { accounts = it }.onFailure { error = it.message ?: "Could not load accounts" }
    }

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
                if (kind == EngagersKind.Favourites) "Favourited by" else "Boosted by",
                style = NeonTheme.type.titleMedium,
                color = palette.text,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            when {
                error != null -> ErrorPane(
                    message = error!!,
                    modifier = Modifier.height(220.dp),
                    onRetry = { retryToken++ },
                )
                accounts == null -> Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = palette.cyan)
                }
                accounts!!.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (kind == EngagersKind.Favourites) "No favourites yet" else "No boosts yet",
                        style = NeonTheme.type.bodyMedium,
                        color = palette.textMute,
                    )
                }
                else -> LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(accounts!!, key = { it.id }) { account -> AccountRow(account = account) }
                }
            }
        }
    }
}

@Composable
fun SheetOption(
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
