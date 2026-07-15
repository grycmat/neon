package com.gigapingu.neon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.theme.NeonAccents
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.ui.LocalNeonNavigator
import com.gigapingu.neon.feature.explore.ExploreScreen
import com.gigapingu.neon.feature.notifications.NotificationsScreen
import com.gigapingu.neon.feature.profile.ProfileScreen
import com.gigapingu.neon.feature.timeline.TimelineScreen

private val TabIcons: List<ImageVector> = listOf(
    Icons.Rounded.Home,
    Icons.Rounded.Search,
    Icons.Outlined.NotificationsNone,
    Icons.Outlined.PersonOutline,
)

/**
 * Root shell: glass bottom tab bar (timelines / explore / notifications /
 * profile) + gradient compose FAB. Tab state survives switches via a
 * SaveableStateHolder (the Flutter shell used an IndexedStack).
 */
@Composable
fun HomeShell(viewModel: ShellViewModel) {
    val palette = NeonTheme.palette
    val navigator = LocalNeonNavigator.current
    val me by viewModel.me.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val stateHolder = rememberSaveableStateHolder()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                stateHolder.SaveableStateProvider(key = "tab-$tab") {
                    when (tab) {
                        0 -> TimelineScreen()
                        1 -> ExploreScreen()
                        2 -> NotificationsScreen()
                        else -> me?.let { ProfileScreen(accountId = it.id, isRoot = true) }
                    }
                }
            }
            TabBar(index = tab, onChanged = { tab = it })
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
