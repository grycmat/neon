package com.gigapingu.neon

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.NeonLabel
import com.gigapingu.neon.core.ui.PreviewHarness
import com.gigapingu.neon.core.designsystem.theme.NeonAccents
import com.gigapingu.neon.core.designsystem.theme.NeonMotion
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.ui.LocalNeonNavigator
import com.gigapingu.neon.feature.explore.ExploreScreen
import com.gigapingu.neon.feature.notifications.NotificationsScreen
import com.gigapingu.neon.feature.profile.ProfileScreen
import com.gigapingu.neon.feature.timeline.TimelineScreen
import kotlin.math.abs
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
                // Continuous page position so the pill tracks the swipe live.
                position = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                    .coerceIn(0f, (TabIcons.size - 1).toFloat()),
                onChanged = { page ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                }
            )
        }
        ComposeFab(
            onClick = { navigator.openCompose() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp),
        )
    }
}

/** Gradient compose FAB with springy press feedback. */
@Composable
private fun ComposeFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = NeonTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .86f else 1f,
        animationSpec = NeonMotion.bouncy(),
        label = "fabScale",
    )
    Box(
        modifier = modifier
            .size(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = NeonAccents.Purple.copy(alpha = .5f),
                spotColor = NeonAccents.Purple.copy(alpha = .5f),
            )
            .clip(RoundedCornerShape(20.dp))
            .background(palette.gradient)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
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

@Composable
private fun TopAppBar(page: Int, onSettingsClick: () -> Unit) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type

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
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    // Title rolls in the swipe direction.
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInVertically(NeonMotion.quick()) { dir * it / 2 } +
                        fadeIn(NeonMotion.quick())) togetherWith
                        (slideOutVertically(NeonMotion.quick()) { -dir * it / 2 } +
                            fadeOut(NeonMotion.quick()))
                },
                label = "topBarTitle",
                modifier = Modifier.weight(1f),
            ) { p ->
                val (title, icon) = when (p) {
                    0 -> Pair("Home", Icons.Rounded.Home)
                    1 -> Pair("Explore", Icons.Rounded.Search)
                    2 -> Pair("Notifications", Icons.Outlined.NotificationsNone)
                    else -> Pair("Profile", Icons.Outlined.PersonOutline)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    if (p == 2) {
                        Spacer(Modifier.width(8.dp))
                        NeonLabel("Live")
                    }
                }
            }
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
private fun TabBar(position: Float, onChanged: (Int) -> Unit) {
    val palette = NeonTheme.palette
    val pillShape = RoundedCornerShape(15.dp)
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
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 4.dp)
                .navigationBarsPadding()
                .padding(bottom = 6.dp),
        ) {
            val tabWidth = maxWidth / TabIcons.size
            // The active pill glides under the icons, tracking the pager.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = tabWidth * position + (tabWidth - 44.dp) / 2)
                    .size(44.dp)
                    .clip(pillShape)
                    .background(palette.cyan.copy(alpha = .1f))
                    .border(1.dp, palette.cyan.copy(alpha = .25f), pillShape),
            )
            Row(Modifier.fillMaxWidth()) {
                TabIcons.forEachIndexed { i, icon ->
                    // 1 when the pill is centred on this tab, 0 a full tab away.
                    val focus = (1f - abs(i - position)).coerceIn(0f, 1f)
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
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = lerp(palette.textMute, palette.cyan, focus),
                            modifier = Modifier.size(23.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Top app bar", showBackground = true, heightDp = 200)
@Composable
private fun TopAppBarPreview() {
    PreviewHarness {
        Column {
            TopAppBar(page = 0, onSettingsClick = {})
            Spacer(Modifier.height(12.dp))
            TopAppBar(page = 2, onSettingsClick = {})
        }
    }
}

@Preview(name = "Tab bar", showBackground = true, heightDp = 200)
@Composable
private fun TabBarPreview() {
    PreviewHarness {
        Column {
            TabBar(position = 0f, onChanged = {})
            Spacer(Modifier.height(12.dp))
            // Mid-swipe between Explore and Notifications.
            TabBar(position = 1.5f, onChanged = {})
        }
    }
}

@Preview(name = "Compose FAB", showBackground = true, heightDp = 120)
@Composable
private fun ComposeFabPreview() {
    PreviewHarness {
        Box(Modifier.fillMaxSize()) {
            ComposeFab(onClick = {}, modifier = Modifier.align(Alignment.Center))
        }
    }
}
