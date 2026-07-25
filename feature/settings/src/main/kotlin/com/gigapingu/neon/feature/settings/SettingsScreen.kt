package com.gigapingu.neon.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Smartphone
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.gigapingu.neon.core.data.ThemeMode
import com.gigapingu.neon.core.designsystem.component.GlassButton
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
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
    val prefNotificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val alertPrefs by viewModel.notificationAlertPrefs.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.setNotificationsEnabled(true)
        }
    }

    val isToggled = prefNotificationsEnabled && hasPermission

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
            NeonLabel("Notifications", modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Push notifications", style = type.titleSmall, color = palette.text)
                        Text("Receive notifications on your device", style = type.bodySmall, color = palette.textDim)
                    }
                    Switch(
                        checked = isToggled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setNotificationsEnabled(true)
                                }
                            } else {
                                viewModel.setNotificationsEnabled(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = palette.cyan.copy(alpha = .35f),
                            checkedThumbColor = palette.cyan,
                        ),
                    )
                }
            }
            if (isToggled) {
                Spacer(Modifier.height(10.dp))
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                    Column {
                        NotificationAlertToggle("Mentions", alertPrefs.mention) {
                            viewModel.setNotificationAlertPrefs(alertPrefs.copy(mention = it))
                        }
                        NotificationAlertToggle("Favourites", alertPrefs.favourite) {
                            viewModel.setNotificationAlertPrefs(alertPrefs.copy(favourite = it))
                        }
                        NotificationAlertToggle("Boosts", alertPrefs.reblog) {
                            viewModel.setNotificationAlertPrefs(alertPrefs.copy(reblog = it))
                        }
                        NotificationAlertToggle("New followers", alertPrefs.follow) {
                            viewModel.setNotificationAlertPrefs(alertPrefs.copy(follow = it))
                        }
                        NotificationAlertToggle("Follow requests", alertPrefs.followRequest) {
                            viewModel.setNotificationAlertPrefs(alertPrefs.copy(followRequest = it))
                        }
                        NotificationAlertToggle("Polls", alertPrefs.poll) {
                            viewModel.setNotificationAlertPrefs(alertPrefs.copy(poll = it))
                        }
                        NotificationAlertToggle("New posts", alertPrefs.status, isLast = true) {
                            viewModel.setNotificationAlertPrefs(alertPrefs.copy(status = it))
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            NeonLabel("Composing", modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                Column {
                    Text("Default visibility", style = type.bodySmall, color = palette.textDim)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PostVisibilityOptions.forEach { visibility ->
                            VisibilityChip(
                                visibility = visibility,
                                selected = (me?.source?.privacy ?: "public") == visibility,
                                onSelect = { viewModel.setDefaultPrivacy(visibility) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Default language (ISO code)", style = type.bodySmall, color = palette.textDim)
                    Spacer(Modifier.height(8.dp))
                    var language by remember(me?.source?.language) {
                        mutableStateOf(me?.source?.language.orEmpty())
                    }
                    BasicTextField(
                        value = language,
                        onValueChange = { language = it },
                        textStyle = type.bodyMedium.copy(color = palette.text),
                        cursorBrush = SolidColor(palette.cyan),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.surface, RoundedCornerShape(8.dp))
                            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        decorationBox = { inner ->
                            if (language.isEmpty()) {
                                Text("e.g. en", style = type.bodyMedium, color = palette.textMute)
                            }
                            inner()
                        },
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.setDefaultLanguage(language) },
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    GlassButton(
                        label = "Save language",
                        onClick = { viewModel.setDefaultLanguage(language) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
                label = "Server info",
                onClick = {
                    val instance = viewModel.instance
                    if (instance != null) {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://$instance/about"),
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            GlassButton(
                label = "Bookmarks",
                onClick = Navigator::openBookmarks,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            GlassButton(
                label = "Lists",
                onClick = Navigator::openManageLists,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            GlassButton(
                label = "Followed hashtags",
                onClick = Navigator::openManageFollowedHashtags,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            GlassButton(
                label = "Filters",
                onClick = Navigator::openFilters,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            GlassButton(
                label = "Log out",
                onClick = viewModel::logout,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NotificationAlertToggle(
    label: String,
    checked: Boolean,
    isLast: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isLast) 0.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = type.bodyMedium, color = palette.text, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = palette.cyan.copy(alpha = .35f),
                checkedThumbColor = palette.cyan,
            ),
        )
    }
}

@Composable
private fun VisibilityChip(
    visibility: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) palette.cyan.copy(alpha = .1f) else palette.surface, shape)
            .border(1.dp, if (selected) palette.cyan.copy(alpha = .4f) else palette.border, shape)
            .clickable { onSelect() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            visibility.replaceFirstChar { it.uppercase() },
            style = type.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = if (selected) palette.cyan else palette.textDim,
        )
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
