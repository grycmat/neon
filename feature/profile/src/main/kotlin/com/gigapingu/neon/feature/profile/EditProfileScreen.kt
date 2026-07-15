package com.gigapingu.neon.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassButton
import com.gigapingu.neon.core.designsystem.component.GlassField
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.GradientButton
import com.gigapingu.neon.core.designsystem.component.NeonAvatar
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.htmlToPlainText
import com.gigapingu.neon.core.ui.LocalNeonNavigator

/**
 * Edit the logged user's profile: display name, bio, avatar/header, lock
 * account. Saves via update_credentials and syncs the cached self account.
 */
@Composable
fun EditProfileScreen(viewModel: EditProfileViewModel = hiltViewModel()) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val navigator = LocalNeonNavigator.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val account = viewModel.me

    LaunchedEffect(Unit) { viewModel.start(htmlToPlainText(account?.note.orEmpty())) }
    LaunchedEffect(Unit) { viewModel.errors.collect { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(uiState.done) { if (uiState.done) navigator.back() }

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { viewModel.onAvatarPicked(it) }
    val headerPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { viewModel.onHeaderPicked(it) }

    NeonBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                    onClick = navigator::back,
                    contentDescription = "Back",
                )
                Spacer(Modifier.width(10.dp))
                Text("Edit profile", style = type.headlineMedium, color = palette.text)
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, top = 10.dp, end = 22.dp, bottom = 30.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        NeonAvatar(account = account, size = 76.dp, ring = true)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(palette.gradient)
                                .border(2.dp, palette.bg, CircleShape)
                                .clickable {
                                    avatarPicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                                        ),
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.CameraAlt,
                                contentDescription = "Change avatar",
                                tint = palette.onGradient,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            uiState.avatarName?.let { "New avatar selected: $it" }
                                ?: "Tap the badge to change your avatar",
                            style = type.bodySmall,
                            color = palette.textDim,
                        )
                        Spacer(Modifier.height(8.dp))
                        GlassButton(
                            label = uiState.headerName?.let { "Header: $it" } ?: "Change header image",
                            height = 38.dp,
                            onClick = {
                                headerPicker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))
                GlassField(label = "Display name") {
                    BasicTextField(
                        value = uiState.displayName,
                        onValueChange = viewModel::onDisplayNameChange,
                        singleLine = true,
                        textStyle = type.bodyLarge.copy(color = palette.text, fontWeight = FontWeight.SemiBold),
                        cursorBrush = SolidColor(palette.cyan),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(12.dp))
                GlassField(label = "Bio") {
                    BasicTextField(
                        value = uiState.bio,
                        onValueChange = viewModel::onBioChange,
                        minLines = 5,
                        textStyle = type.bodyLarge.copy(color = palette.text),
                        cursorBrush = SolidColor(palette.cyan),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (uiState.bio.isEmpty()) {
                                Text(
                                    "A few lines in your own voice…",
                                    style = type.bodyLarge,
                                    color = palette.textMute,
                                )
                            }
                            inner()
                        },
                    )
                }
                Spacer(Modifier.height(12.dp))
                val shape = RoundedCornerShape(16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(palette.surface)
                        .border(1.dp, palette.border, shape)
                        .padding(horizontal = 15.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Locked account", style = type.titleSmall, color = palette.text)
                        Text("Manually approve new followers", style = type.bodySmall, color = palette.textDim)
                    }
                    Switch(
                        checked = uiState.locked,
                        onCheckedChange = viewModel::onLockedChange,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = palette.cyan.copy(alpha = .35f),
                            checkedThumbColor = palette.cyan,
                        ),
                    )
                }
                Spacer(Modifier.height(26.dp))
                GradientButton(
                    label = "Save changes",
                    busy = uiState.saving,
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
