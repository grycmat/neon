package com.gigapingu.neon.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gigapingu.neon.core.data.AsyncPhase
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.designsystem.component.EmojiText
import com.gigapingu.neon.core.designsystem.component.GlassButton
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.GradientButton
import com.gigapingu.neon.core.designsystem.component.HtmlText
import com.gigapingu.neon.core.designsystem.component.NeonAvatar
import com.gigapingu.neon.core.designsystem.component.NeonLabel
import com.gigapingu.neon.core.designsystem.theme.NeonDims
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.compactCount
import com.gigapingu.neon.core.model.FeaturedTag
import com.gigapingu.neon.core.model.MediaAttachment
import com.gigapingu.neon.core.model.Relationship
import com.gigapingu.neon.core.ui.AsyncList
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.LocalShellPadding
import com.gigapingu.neon.core.ui.PreviewFixtures
import com.gigapingu.neon.core.ui.PreviewHarness
import com.gigapingu.neon.core.ui.StatusActionService
import com.gigapingu.neon.core.ui.hingePaneWidth
import com.gigapingu.neon.core.ui.isBigScreen
import com.gigapingu.neon.core.ui.status.StatusCard

/**
 * Profile — used for the logged user (isRoot, in the tab bar) and any author
 * opened from a toot. Self gets Edit profile; others get Follow.
 */
@Composable
fun ProfileScreen(
    accountId: String,
    isRoot: Boolean = false,
    viewModel: ProfileViewModel = hiltViewModel(key = "profile-$accountId"),
) {
    val palette = NeonTheme.palette
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val shellPadding = LocalShellPadding.current
    var showListSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportComment by remember { mutableStateOf("") }
    val onOpenListMembership: (() -> Unit)? =
        if (!uiState.isSelf && uiState.account != null) ({ showListSheet = true }) else null

    LaunchedEffect(accountId) { viewModel.start(accountId) }

    val modifier = if (isRoot) Modifier.fillMaxSize() else Modifier.fillMaxSize().statusBarsPadding()
    val listState = AsyncState(
        phase = if (uiState.loadingStatuses) AsyncPhase.Loading else AsyncPhase.Ready,
        data = if (uiState.account == null) null else uiState.statuses,
        hasMore = uiState.hasMore,
    )
    if (isBigScreen()) {
        // Identity column left of the hinge, toots column right (design 06).
        Row(modifier) {
            Column(
                Modifier
                    .width(hingePaneWidth(inShell = isRoot))
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        top = 4.dp + shellPadding.calculateTopPadding(),
                        end = 16.dp,
                        bottom = 30.dp + shellPadding.calculateBottomPadding(),
                    ),
            ) {
                if (!isRoot) {
                    TopBar(onOpenListMembership = onOpenListMembership)
                }
                if (uiState.account != null) {
                    ProfileHeader(
                        uiState = uiState,
                        onToggleFollow = viewModel::toggleFollow,
                        onToggleMute = viewModel::toggleMute,
                        onToggleBlock = viewModel::toggleBlock,
                        onRequestReport = { showReportDialog = true },
                        onAddFeaturedTag = viewModel::addFeaturedTag,
                        onRemoveFeaturedTag = viewModel::removeFeaturedTag,
                    )
                } else {
                    Box(
                        Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = palette.cyan)
                    }
                }
            }
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(palette.divider),
            )
            Box(Modifier.weight(1f).fillMaxHeight()) {
                AsyncList(
                    state = listState,
                    onRefresh = viewModel::load,
                    onLoadMore = viewModel::loadMore,
                    emptyLabel = "No toots yet",
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 4.dp + shellPadding.calculateTopPadding(),
                        end = 16.dp,
                        bottom = 90.dp + shellPadding.calculateBottomPadding(),
                    ),
                    key = { it.id },
                    header = {
                        Column {
                            uiState.pinnedStatuses.forEach { status ->
                                StatusCard(status = status, pinned = true)
                            }
                            NeonLabel(
                                "Toots",
                                modifier = Modifier.padding(start = 6.dp, top = 12.dp, end = 6.dp, bottom = 8.dp),
                            )
                        }
                    },
                ) { status ->
                    StatusCard(status = status)
                }
            }
        }
    } else {
        Column(modifier) {
            AsyncList(
                state = listState,
                onRefresh = viewModel::load,
                onLoadMore = viewModel::loadMore,
                emptyLabel = "No toots yet",
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 4.dp + shellPadding.calculateTopPadding(),
                    end = 16.dp,
                    bottom = 90.dp + shellPadding.calculateBottomPadding(),
                ),
                key = { it.id },
                header = {
                    Column {
                        if (!isRoot) {
                            TopBar(onOpenListMembership = onOpenListMembership)
                        }
                        uiState.account?.let {
                            ProfileHeader(
                                uiState = uiState,
                                onToggleFollow = viewModel::toggleFollow,
                                onToggleMute = viewModel::toggleMute,
                                onToggleBlock = viewModel::toggleBlock,
                                onRequestReport = { showReportDialog = true },
                                onAddFeaturedTag = viewModel::addFeaturedTag,
                                onRemoveFeaturedTag = viewModel::removeFeaturedTag,
                            )
                        }
                        uiState.pinnedStatuses.forEach { status ->
                            StatusCard(status = status, pinned = true)
                        }
                        NeonLabel(
                            "Toots",
                            modifier = Modifier.padding(start = 6.dp, top = 20.dp, end = 6.dp, bottom = 8.dp),
                        )
                        if (uiState.loadingStatuses) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(30.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = palette.cyan)
                            }
                        }
                    }
                },
            ) { status ->
                StatusCard(status = status)
            }
        }
    }

    if (showListSheet) {
        ListMembershipSheet(accountId = accountId, onDismiss = { showListSheet = false })
    }
    if (showReportDialog) {
        val account = uiState.account
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report @${account?.acct.orEmpty()}", color = palette.text) },
            text = {
                Column {
                    Text(
                        "Please provide an optional comment for the moderators:",
                        color = palette.textDim,
                        style = NeonTheme.type.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(
                        value = reportComment,
                        onValueChange = { reportComment = it },
                        textStyle = NeonTheme.type.bodyMedium.copy(color = palette.text),
                        cursorBrush = SolidColor(palette.cyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.surface, RoundedCornerShape(8.dp))
                            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        decorationBox = { inner ->
                            if (reportComment.isEmpty()) {
                                Text("Write comment here...", style = NeonTheme.type.bodyMedium, color = palette.textMute)
                            }
                            inner()
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        account?.let { StatusActionService.reportAccount(it, reportComment) }
                        showReportDialog = false
                        reportComment = ""
                    },
                ) {
                    Text("Report", color = palette.pink)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReportDialog = false
                        reportComment = ""
                    },
                ) {
                    Text("Cancel", color = palette.textMute)
                }
            },
            containerColor = palette.surfaceSolid,
            shape = RoundedCornerShape(20.dp),
        )
    }
}

@Composable
private fun TopBar(onOpenListMembership: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
            onClick = Navigator::back,
            contentDescription = "Back",
        )
        if (onOpenListMembership != null) {
            Spacer(Modifier.weight(1f))
            GlassIconButton(
                icon = Icons.Rounded.MoreVert,
                onClick = onOpenListMembership,
                contentDescription = "Add to list",
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    uiState: ProfileUiState,
    onToggleFollow: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleBlock: () -> Unit,
    onRequestReport: () -> Unit = {},
    onAddFeaturedTag: (String) -> Unit = {},
    onRemoveFeaturedTag: (FeaturedTag) -> Unit = {},
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val account = uiState.account ?: return
    val rel = uiState.relationship
    val following = rel?.following == true
    val requested = rel?.requested == true

    val bannerHeight = 130.dp
    val avatarSize = 84.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(bannerHeight + avatarSize / 2),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .clip(RoundedCornerShape(NeonDims.RadiusCard))
                .then(if (account.hasCustomHeader) Modifier else Modifier.background(palette.gradient))
                .then(
                    if (account.hasCustomHeader) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            Navigator.openMediaPreview(
                                listOf(MediaAttachment(id = "header", rawType = "image", url = account.header)),
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (account.hasCustomHeader) {
                AsyncImage(
                    model = account.header,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        NeonAvatar(
            account = account,
            size = avatarSize,
            ring = true,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp)
                .then(
                    if (account.avatar.isNotBlank()) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            Navigator.openMediaPreview(
                                listOf(MediaAttachment(id = "avatar", rawType = "image", url = account.avatar)),
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        )
    }
    Spacer(Modifier.height(8.dp))

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(18.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                when {
                    uiState.isSelf -> GlassButton(
                        label = "Edit profile",
                        height = 40.dp,
                        tinted = true,
                        onClick = Navigator::openEditProfile,
                    )
                    rel != null -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GlassIconButton(
                                icon = if (rel.muting) Icons.Rounded.VolumeMute else Icons.Rounded.VolumeUp,
                                tinted = rel.muting,
                                onClick = onToggleMute,
                                contentDescription = if (rel.muting) "Unmute" else "Mute",
                            )
                            Spacer(Modifier.width(8.dp))
                            GlassIconButton(
                                icon = Icons.Rounded.Block,
                                tinted = rel.blocking,
                                onClick = onToggleBlock,
                                contentDescription = if (rel.blocking) "Unblock" else "Block",
                            )
                            Spacer(Modifier.width(8.dp))
                            GlassIconButton(
                                icon = Icons.Rounded.Report,
                                onClick = onRequestReport,
                                contentDescription = "Report @${account.acct}",
                            )
                            Spacer(Modifier.width(8.dp))
                            if (following || requested) {
                                GlassButton(
                                    label = if (requested) "Requested" else "Following",
                                    height = 40.dp,
                                    onClick = if (uiState.followBusy) null else onToggleFollow,
                                    modifier = Modifier.width(118.dp),
                                )
                            } else {
                                GradientButton(
                                    label = "Follow",
                                    height = 40.dp,
                                    busy = uiState.followBusy,
                                    onClick = onToggleFollow,
                                    modifier = Modifier.width(118.dp),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            EmojiText(account.displayNameOrUsername, emojis = account.emojis, style = type.displaySmall, color = palette.text)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    account.fullHandle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = type.bodyMedium,
                    color = palette.textDim,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (rel?.followedBy == true) {
                    Spacer(Modifier.width(8.dp))
                    val badgeShape = RoundedCornerShape(8.dp)
                    Box(
                        modifier = Modifier
                            .clip(badgeShape)
                            .background(palette.cyan.copy(alpha = .08f))
                            .border(1.dp, palette.cyan.copy(alpha = .3f), badgeShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text("Follows you", style = type.labelSmall, color = palette.cyan)
                    }
                }
            }
            if (account.note.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HtmlText(
                    account.note,
                    style = type.bodyMedium,
                    emojis = account.emojis,
                    onHashtagClick = { tag -> Navigator.openHashtagSearch(tag) },
                    onMentionClick = { acctOrUrl -> StatusActionService.openMention(acctOrUrl) },
                    onLinkClick = { url -> StatusActionService.openUrl(url) },
                )
            }
            if (uiState.featuredTags.isNotEmpty() || uiState.isSelf) {
                Spacer(Modifier.height(12.dp))
                FeaturedTagsRow(
                    tags = uiState.featuredTags,
                    editable = uiState.isSelf,
                    onAdd = onAddFeaturedTag,
                    onRemove = onRemoveFeaturedTag,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stat(compactCount(account.statusesCount), "Toots")
                StatDivider()
                Stat(compactCount(account.followersCount), "Followers") {
                    Navigator.openFollowList(account.id, account.fullHandle, following = false)
                }
                StatDivider()
                Stat(compactCount(account.followingCount), "Following") {
                    Navigator.openFollowList(account.id, account.fullHandle, following = true)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeaturedTagsRow(
    tags: List<FeaturedTag>,
    editable: Boolean,
    onAdd: (String) -> Unit,
    onRemove: (FeaturedTag) -> Unit,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    var showAddDialog by remember { mutableStateOf(false) }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            val shape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(palette.purple.copy(alpha = .1f))
                    .border(1.dp, palette.purple.copy(alpha = .3f), shape)
                    .clickable { Navigator.openHashtag(tag.name) }
                    .padding(start = 11.dp, end = if (editable) 6.dp else 11.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "#${tag.name}",
                    style = type.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.purple,
                )
                if (editable) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Unfeature #${tag.name}",
                        tint = palette.purple,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(role = Role.Button) { onRemove(tag) },
                    )
                }
            }
        }
        if (editable) {
            val shape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .clip(shape)
                    .border(1.dp, palette.border, shape)
                    .clickable(role = Role.Button) { showAddDialog = true }
                    .padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = palette.textDim, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Feature hashtag", style = type.bodySmall, color = palette.textDim)
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Feature a hashtag", color = palette.text) },
            text = {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = type.bodyMedium.copy(color = palette.text),
                    cursorBrush = SolidColor(palette.cyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    decorationBox = { inner ->
                        if (name.isEmpty()) {
                            Text("hashtag", style = type.bodyMedium, color = palette.textMute)
                        }
                        inner()
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAdd(name)
                    showAddDialog = false
                }) {
                    Text("Add", color = palette.cyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = palette.textMute)
                }
            },
            containerColor = palette.surfaceSolid,
            shape = RoundedCornerShape(20.dp),
        )
    }
}

@Composable
private fun Stat(value: String, label: String, onClick: (() -> Unit)? = null) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    Column(
        modifier = if (onClick != null) {
            Modifier.clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
        } else {
            Modifier
        },
    ) {
        Text(value, style = type.titleMedium.copy(fontSize = 17.sp), color = palette.text)
        Text(label, style = type.bodySmall, color = palette.textDim)
    }
}

@Composable
private fun StatDivider() {
    Box(
        Modifier
            .padding(horizontal = 18.dp)
            .width(1.dp)
            .height(26.dp)
            .background(NeonTheme.palette.divider),
    )
}

@Preview(name = "Profile header — other user", showBackground = true, heightDp = 420)
@Composable
private fun ProfileHeaderPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            ProfileHeader(
                uiState = ProfileUiState(
                    account = PreviewFixtures.account,
                    relationship = Relationship(id = "1", followedBy = true),
                ),
                onToggleFollow = {},
                onToggleMute = {},
                onToggleBlock = {},
            )
        }
    }
}

@Preview(name = "Profile header — self + following", showBackground = true, heightDp = 1100)
@Composable
private fun ProfileHeaderVariantsPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            ProfileHeader(
                uiState = ProfileUiState(account = PreviewFixtures.account, isSelf = true),
                onToggleFollow = {},
                onToggleMute = {},
                onToggleBlock = {},
            )
            ProfileHeader(
                uiState = ProfileUiState(
                    account = PreviewFixtures.account2,
                    relationship = Relationship(id = "2", following = true),
                ),
                onToggleFollow = {},
                onToggleMute = {},
                onToggleBlock = {},
            )
            ProfileHeader(
                uiState = ProfileUiState(
                    account = PreviewFixtures.account.copy(header = "https://picsum.photos/id/1015/800/400"),
                    isSelf = true,
                ),
                onToggleFollow = {},
                onToggleMute = {},
                onToggleBlock = {},
            )
        }
    }
}
