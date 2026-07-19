package com.gigapingu.neon.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.data.ThemeMode
import com.gigapingu.neon.core.designsystem.component.GlassButton
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.component.NeonLabel
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.PreviewHarness

/** Settings — theme mode + account/session. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val mode by viewModel.themeMode.collectAsStateWithLifecycle()
    val me by viewModel.me.collectAsStateWithLifecycle()

    NeonBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                    onClick = Navigator::back,
                    contentDescription = "Back",
                )
                Spacer(Modifier.width(10.dp))
                Text("Settings", style = type.headlineMedium, color = palette.text)
            }
            Column(
                Modifier
                    // Cap + centre on big screens; no-op at phone widths.
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 30.dp),
            ) {
                NeonLabel("Appearance", modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeOption(
                        mode = ThemeMode.Dark,
                        label = "Neon dark",
                        icon = Icons.Rounded.DarkMode,
                        current = mode,
                        onSelect = viewModel::setThemeMode,
                        modifier = Modifier.weight(1f),
                    )
                    ThemeOption(
                        mode = ThemeMode.Light,
                        label = "Light",
                        icon = Icons.Rounded.LightMode,
                        current = mode,
                        onSelect = viewModel::setThemeMode,
                        modifier = Modifier.weight(1f),
                    )
                    ThemeOption(
                        mode = ThemeMode.System,
                        label = "System",
                        icon = Icons.Rounded.Smartphone,
                        current = mode,
                        onSelect = viewModel::setThemeMode,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(28.dp))
                NeonLabel("Account", modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 10.dp))
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                    Column {
                        Text(me?.fullHandle.orEmpty(), style = type.titleSmall, color = palette.text)
                        Text(viewModel.instance.orEmpty(), style = type.bodySmall, color = palette.textDim)
                    }
                }
                Spacer(Modifier.height(14.dp))
                GlassButton(
                    label = "Log out",
                    onClick = viewModel::logout,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    label: String,
    icon: ImageVector,
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val selected = mode == current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) palette.cyan.copy(alpha = .08f) else palette.surface)
            .border(1.dp, if (selected) palette.cyan.copy(alpha = .4f) else palette.border, shape)
            .clickable { onSelect(mode) }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) palette.cyan else palette.textDim,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = type.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = if (selected) palette.text else palette.textDim,
        )
    }
}

@Preview(name = "Theme options", showBackground = true, heightDp = 140)
@Composable
private fun ThemeOptionPreview() {
    PreviewHarness {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ThemeOption(
                mode = ThemeMode.Dark,
                label = "Neon dark",
                icon = Icons.Rounded.DarkMode,
                current = ThemeMode.Dark,
                onSelect = {},
                modifier = Modifier.weight(1f),
            )
            ThemeOption(
                mode = ThemeMode.Light,
                label = "Light",
                icon = Icons.Rounded.LightMode,
                current = ThemeMode.Dark,
                onSelect = {},
                modifier = Modifier.weight(1f),
            )
            ThemeOption(
                mode = ThemeMode.System,
                label = "System",
                icon = Icons.Rounded.Smartphone,
                current = ThemeMode.Dark,
                onSelect = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}
