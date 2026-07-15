package com.gigapingu.neon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.NeonLabel
import com.gigapingu.neon.core.designsystem.theme.NeonAccents
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.ui.LocalNeonNavigator
import com.gigapingu.neon.feature.explore.ExploreScreen
import com.gigapingu.neon.feature.notifications.NotificationsScreen
import com.gigapingu.neon.feature.profile.ProfileScreen
import com.gigapingu.neon.feature.timeline.TimelineScreen
import kotlinx.coroutines.launch

private val TabIcons: List<ImageVector> = listOf(
    Icons.Rounded.Home,
    Icons.Rounded.Search,
    Icons.Outlined.NotificationsNone,
    Icons.Outlined.PersonOutline,
)

/**
 * Root shell: glass bottom tab bar (timelines / explore / notifications /
 * profile) + gradient compose FAB.
 */
@Composable
fun HomeShell(viewModel: ShellViewModel) {
    val palette = NeonTheme.palette
    val navigator = LocalNeonNavigator.current
    val me by viewModel.me.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = 0) { 4 }
    val coroutineScope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                page = pagerState.currentPage,
                onSettingsClick = { navigator.openSettings() }
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 3,
            ) { page ->
                when (page) {
                    0 -> TimelineScreen()
                    1 -> ExploreScreen()
                    2 -> NotificationsScreen()
                    else -> me?.let { ProfileScreen(accountId = it.id, isRoot = true) }
                }
            }
            TabBar(
                index = pagerState.currentPage,
                onChanged = { page ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                }
            )
        }
        // Gradient compose FAB.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp)
                .size(58.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = NeonAccents.Purple.copy(alpha = .5f),
                    spotColor = NeonAccents.Purple.copy(alpha = .5f),
                )
                .clip(RoundedCornerShape(20.dp))
                .background(palette.gradient)
                .clickable { navigator.openCompose() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Edit,
                contentDescription = "Compose",
                tint = palette.onGradient,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun TopAppBar(page: Int, onSettingsClick: () -> Unit) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val (title, icon) = when (page) {
        0 -> Pair("Home", Icons.Rounded.Home)
        1 -> Pair("Explore", Icons.Rounded.Search)
        2 -> Pair("Notifications", Icons.Outlined.NotificationsNone)
        else -> Pair("Profile", Icons.Outlined.PersonOutline)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(palette.surfaceSolid.copy(alpha = .72f))
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = palette.cyan,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                style = type.headlineMedium,
                color = palette.text,
            )
            if (page == 2) {
                Spacer(Modifier.width(8.dp))
                NeonLabel("Live")
            }
            Spacer(Modifier.weight(1f))
            GlassIconButton(
                icon = Icons.Outlined.Settings,
                onClick = onSettingsClick,
                contentDescription = "Settings",
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider),
        )
    }
}

@Composable
private fun TabBar(index: Int, onChanged: (Int) -> Unit) {
    val palette = NeonTheme.palette
    Column(
        Modifier
            .fillMaxWidth()
            .background(palette.surfaceSolid.copy(alpha = .72f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 4.dp)
                .navigationBarsPadding()
                .padding(bottom = 6.dp),
        ) {
            TabIcons.forEachIndexed { i, icon ->
                val active = i == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onChanged(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .then(
                                if (active) {
                                    Modifier
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(palette.cyan.copy(alpha = .1f))
                                        .border(
                                            1.dp,
                                            palette.cyan.copy(alpha = .25f),
                                            RoundedCornerShape(15.dp),
                                        )
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (active) palette.cyan else palette.textMute,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                }
            }
        }
    }
}
