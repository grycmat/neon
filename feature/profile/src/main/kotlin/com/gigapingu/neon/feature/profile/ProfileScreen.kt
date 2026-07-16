package com.gigapingu.neon.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.data.AsyncPhase
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.designsystem.component.GlassButton
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.GradientButton
import com.gigapingu.neon.core.designsystem.component.HtmlText
import com.gigapingu.neon.core.designsystem.component.NeonAvatar
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.component.NeonLabel
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.compactCount
import com.gigapingu.neon.core.ui.AsyncList
import com.gigapingu.neon.core.ui.LocalNeonNavigator
import com.gigapingu.neon.core.ui.status.StatusCard

/**
 * Profile — used for the logged user (isRoot, in the tab bar) and any author
 * opened from a toot. Self gets Edit profile; others get Follow.
 */
@Composable
fun ProfileScreen(
    accountId: String,
    isRoot: Boolean = false,
    heroKey: String? = null,
    viewModel: ProfileViewModel = hiltViewModel(key = "profile-$accountId"),
) {
    val palette = NeonTheme.palette
    val navigator = LocalNeonNavigator.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(accountId) { viewModel.start(accountId) }

    NeonBackground {
        val modifier = if (isRoot) Modifier.fillMaxSize() else Modifier.fillMaxSize().statusBarsPadding()
        Column(modifier) {
            val listState = AsyncState(
                phase = if (uiState.loadingStatuses) AsyncPhase.Loading else AsyncPhase.Ready,
                data = if (uiState.account == null) null else uiState.statuses,
                hasMore = uiState.hasMore,
            )
            AsyncList(
                state = listState,
                onRefresh = viewModel::load,
                onLoadMore = viewModel::loadMore,
                emptyLabel = "No toots yet",
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 90.dp),
                key = { it.id },
                header = {
                    Column {
                        if (!isRoot) {
                            TopBar()
                        }
                        uiState.account?.let { ProfileHeader(uiState, viewModel, heroKey) }
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
}

@Composable
private fun TopBar() {
    val navigator = LocalNeonNavigator.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
            onClick = navigator::back,
            contentDescription = "Back",
        )
    }
}

@Composable
private fun ProfileHeader(
    uiState: ProfileUiState,
    viewModel: ProfileViewModel,
    heroKey: String? = null,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val navigator = LocalNeonNavigator.current
    val account = uiState.account ?: return
    val rel = uiState.relationship
    val following = rel?.following == true
    val requested = rel?.requested == true

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(18.dp),
    ) {
        Column {
            Row {
                NeonAvatar(account = account, size = 72.dp, ring = true, heroKey = heroKey)
                Spacer(Modifier.weight(1f))
                when {
                    uiState.isSelf -> GlassButton(
                        label = "Edit profile",
                        height = 40.dp,
                        tinted = true,
                        onClick = navigator::openEditProfile,
                    )
                    rel != null -> {
                        if (following || requested) {
                            GlassButton(
                                label = if (requested) "Requested" else "Following",
                                height = 40.dp,
                                onClick = if (uiState.followBusy) null else viewModel::toggleFollow,
                                modifier = Modifier.width(118.dp),
                            )
                        } else {
                            GradientButton(
                                label = "Follow",
                                height = 40.dp,
                                busy = uiState.followBusy,
                                onClick = viewModel::toggleFollow,
                                modifier = Modifier.width(118.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(account.displayNameOrUsername, style = type.displaySmall, color = palette.text)
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
                HtmlText(account.note, style = type.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stat(compactCount(account.statusesCount), "Toots")
                StatDivider()
                Stat(compactCount(account.followersCount), "Followers") {
                    navigator.openFollowList(account.id, account.fullHandle, following = false)
                }
                StatDivider()
                Stat(compactCount(account.followingCount), "Following") {
                    navigator.openFollowList(account.id, account.fullHandle, following = true)
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, onClick: (() -> Unit)? = null) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    Column(
        modifier = if (onClick != null) {
            Modifier.clickable(interactionSource = null, indication = null, onClick = onClick)
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
