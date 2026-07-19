package com.gigapingu.neon

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.component.NeonLabel
import com.gigapingu.neon.core.ui.PreviewHarness
import com.gigapingu.neon.core.designsystem.theme.NeonAccents
import com.gigapingu.neon.core.designsystem.theme.NeonMotion
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.LocalShellPadding
import com.gigapingu.neon.core.ui.ShellRailWidth
import com.gigapingu.neon.core.ui.hingePaneWidth
import com.gigapingu.neon.core.ui.isBigScreen
import com.gigapingu.neon.feature.explore.ExploreScreen
import com.gigapingu.neon.feature.notifications.NotificationsScreen
import com.gigapingu.neon.feature.notifications.NotificationsViewModel
import com.gigapingu.neon.feature.profile.ProfileScreen
import com.gigapingu.neon.feature.thread.ThreadScreen
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
 * Root shell. Phones: glass bottom tab bar (timelines / explore /
 * notifications / profile) + gradient compose FAB. Big screens (unfolded
 * foldables, tablets): a left nav rail replaces both, and the Home /
 * Notifications tabs become hinge-aligned list-detail panes with the thread
 * opening on the right.
 */
@Composable
fun HomeShell(viewModel: ShellViewModel) {
    val me by viewModel.me.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = 0) { 4 }
    val coroutineScope = rememberCoroutineScope()
    val big = isBigScreen()
    var showClearConfirm by remember { mutableStateOf(false) }

    // Detail-pane selection per list-detail tab (big screens only).
    var homeThreadId by rememberSaveable { mutableStateOf<String?>(null) }
    var notifThreadId by rememberSaveable { mutableStateOf<String?>(null) }
    if (big) {
        // Route thread opens from the visible list tab into its detail pane
        // instead of pushing; other tabs keep the full-screen push.
        DisposableEffect(pagerState) {
            Navigator.threadPaneHandler = { statusId ->
                when (pagerState.currentPage) {
                    0 -> {
                        homeThreadId = statusId
                        true
                    }
                    2 -> {
                        notifThreadId = statusId
                        true
                    }
                    else -> false
                }
            }
            onDispose { Navigator.threadPaneHandler = null }
        }
    }

    // Measured bar heights (insets included) feed LocalShellPadding so tab
    // content starts below the floating bars but scrolls behind them.
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val bottomBarHeight = with(density) { bottomBarHeightPx.toDp() }
    val shellPadding = PaddingValues(
        top = with(density) { topBarHeightPx.toDp() },
        bottom = if (big) 0.dp else bottomBarHeight,
    )

    val pager: @Composable () -> Unit = {
        CompositionLocalProvider(LocalShellPadding provides shellPadding) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 3,
            ) { page ->
                when (page) {
                    0 -> if (big) {
                        ShellListDetail(detailId = homeThreadId) {
                            TimelineScreen(selectedStatusId = homeThreadId)
                        }
                    } else {
                        TimelineScreen()
                    }
                    1 -> ExploreScreen()
                    2 -> if (big) {
                        ShellListDetail(detailId = notifThreadId) {
                            NotificationsScreen(selectedStatusId = notifThreadId)
                        }
                    } else {
                        NotificationsScreen()
                    }
                    else -> me?.let { ProfileScreen(accountId = it.id, isRoot = true) }
                }
            }
        }
    }
    val position = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        .coerceIn(0f, (TabIcons.size - 1).toFloat())
    val onTabChanged: (Int) -> Unit = { page ->
        coroutineScope.launch {
            pagerState.animateScrollToPage(page)
        }
    }

    if (big) {
        Row(Modifier.fillMaxSize()) {
            ShellRail(
                position = position,
                onChanged = onTabChanged,
                onCompose = { Navigator.openCompose() },
            )
            Box(Modifier.weight(1f)) {
                pager()
                TopAppBar(
                    page = pagerState.currentPage,
                    onSettingsClick = { Navigator.openSettings() },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .onSizeChanged { topBarHeightPx = it.height },
                )
            }
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            pager()
            TopAppBar(
                page = pagerState.currentPage,
                onSettingsClick = { Navigator.openSettings() },
                onClearClick = { showClearConfirm = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged { topBarHeightPx = it.height },
            )
            TabBar(
                // Continuous page position so the pill tracks the swipe live.
                position = position,
                onChanged = onTabChanged,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { bottomBarHeightPx = it.height },
            )
            ComposeFab(
                onClick = { Navigator.openCompose() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = bottomBarHeight + 16.dp),
            )
        }
    }

    if (showClearConfirm) {
        val notificationsViewModel: NotificationsViewModel = hiltViewModel()
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all notifications", color = NeonTheme.palette.text) },
            text = { Text("Are you sure you want to clear all your notifications?", color = NeonTheme.palette.textDim) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        notificationsViewModel.clearAll()
                        showClearConfirm = false
                    }
                ) {
                    Text("Clear", color = NeonTheme.palette.pink)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = NeonTheme.palette.textMute)
                }
            },
            containerColor = NeonTheme.palette.surfaceSolid,
            shape = RoundedCornerShape(20.dp),
        )
    }
}

/**
 * Big-screen tab body: list pane left of the hinge, thread detail right.
 * Composed here (not in the feature) because list and detail live in
 * different feature modules.
 */
@Composable
private fun ShellListDetail(detailId: String?, list: @Composable () -> Unit) {
    val palette = NeonTheme.palette
    Row(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .width(hingePaneWidth(inShell = true))
                .fillMaxHeight(),
        ) {
            list()
        }
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(palette.divider),
        )
        Box(Modifier.weight(1f).fillMaxHeight()) {
            if (detailId != null) {
                ThreadScreen(statusId = detailId, embedded = true)
            } else {
                NeonBackground {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Outlined.Forum,
                            contentDescription = null,
                            tint = palette.textMute,
                            modifier = Modifier.size(34.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Tap a toot to read the thread here",
                            style = NeonTheme.type.bodyMedium,
                            color = palette.textMute,
                        )
                    }
                }
            }
        }
    }
}

/** Left navigation rail — replaces the bottom tab bar + FAB on big screens. */
@Composable
private fun ShellRail(
    position: Float,
    onChanged: (Int) -> Unit,
    onCompose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NeonTheme.palette
    Row(modifier.fillMaxHeight()) {
        Column(
            Modifier
                .width(ShellRailWidth)
                .fillMaxHeight()
                .background(palette.surfaceSolid)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 6.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp),
                    contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher),
                    contentDescription = "App Icon",
                    modifier = Modifier.size(48.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            val itemShape = RoundedCornerShape(16.dp)
            TabIcons.forEachIndexed { i, icon ->
                // 1 when the pill is centred on this tab, 0 a full tab away.
                val focus = (1f - abs(i - position)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .padding(vertical = 3.dp)
                        .size(48.dp)
                        .clip(itemShape)
                        .background(palette.cyan.copy(alpha = .10f * focus))
                        .border(1.dp, palette.cyan.copy(alpha = .25f * focus), itemShape)
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
            Spacer(Modifier.weight(1f))
            val fabShape = RoundedCornerShape(17.dp)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = fabShape,
                        ambientColor = NeonAccents.Purple.copy(alpha = .45f),
                        spotColor = NeonAccents.Purple.copy(alpha = .45f),
                    )
                    .clip(fabShape)
                    .background(palette.gradient)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCompose,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "Compose",
                    tint = palette.onGradient,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(palette.divider),
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
private fun TopAppBar(
    page: Int,
    onSettingsClick: () -> Unit,
    onClearClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type

    Column(
        modifier
            .fillMaxWidth()
            .background(palette.surfaceSolid.copy(alpha = .60f))
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
            if (page == 2 && onClearClick != null) {
                GlassIconButton(
                    icon = Icons.Rounded.Delete,
                    onClick = onClearClick,
                    contentDescription = "Clear all notifications",
                )
                Spacer(Modifier.width(8.dp))
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
private fun TabBar(position: Float, onChanged: (Int) -> Unit, modifier: Modifier = Modifier) {
    val palette = NeonTheme.palette
    val pillShape = RoundedCornerShape(15.dp)
    Column(
        modifier
            .fillMaxWidth()
            .background(palette.surfaceSolid.copy(alpha = .90f)),
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

@Preview(name = "Nav rail", showBackground = true, widthDp = 120, heightDp = 640)
@Composable
private fun ShellRailPreview() {
    PreviewHarness {
        ShellRail(position = 0f, onChanged = {}, onCompose = {})
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
