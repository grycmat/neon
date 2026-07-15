package com.gigapingu.neon.feature.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gigapingu.neon.core.designsystem.component.GlassField
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.model.MediaAttachment

/** Attached-media strip with per-item alt-text editing and removal. */
@Composable
fun MediaStrip(
    items: List<MediaAttachment>,
    onRemove: (MediaAttachment) -> Unit,
    onEditAlt: (MediaAttachment, String) -> Unit,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    if (items.isEmpty()) return
    var editing by remember { mutableStateOf<MediaAttachment?>(null) }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEach { attachment ->
            Box {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.gradientSoft)
                        .border(1.dp, palette.border, RoundedCornerShape(14.dp)),
                ) {
                    if (attachment.preview.isNotEmpty()) {
                        AsyncImage(
                            model = attachment.preview,
                            contentDescription = attachment.altText.ifEmpty { null },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(92.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = .55f))
                        .clickable { onRemove(attachment) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Remove attachment",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.Black.copy(alpha = .55f))
                        .border(
                            1.dp,
                            if (attachment.altText.isEmpty()) palette.border else palette.cyan.copy(alpha = .6f),
                            RoundedCornerShape(7.dp),
                        )
                        .clickable { editing = attachment }
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        "ALT",
                        style = type.labelSmall,
                        color = if (attachment.altText.isEmpty()) Color.White.copy(alpha = .7f) else palette.cyan,
                    )
                }
            }
        }
    }

    editing?.let { attachment ->
        AltTextSheet(
            attachment = attachment,
            onDismiss = { editing = null },
            onSave = { description ->
                editing = null
                onEditAlt(attachment, description)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AltTextSheet(
    attachment: MediaAttachment,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    var text by remember { mutableStateOf(attachment.altText) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceSolid,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            Text("Describe this image", style = type.headlineMedium, color = palette.text)
            Spacer(Modifier.height(12.dp))
            GlassField {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = type.bodyMedium.copy(color = palette.text),
                    cursorBrush = SolidColor(palette.cyan),
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (text.isEmpty()) {
                            Text(
                                "Alt text for screen readers",
                                style = type.bodyMedium,
                                color = palette.textMute,
                            )
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                com.gigapingu.neon.core.designsystem.component.GlassButton(
                    label = "Cancel",
                    height = 44.dp,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                com.gigapingu.neon.core.designsystem.component.GradientButton(
                    label = "Save",
                    height = 44.dp,
                    onClick = { onSave(text) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Inline poll editor: 2–4 options, duration, single/multiple choice. */
@Composable
fun PollEditor(
    poll: PollDraftState,
    onUpdate: ((PollDraftState) -> PollDraftState) -> Unit,
    onRemove: () -> Unit,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val shape = RoundedCornerShape(18.dp)
    var durationMenuOpen by remember { mutableStateOf(false) }

    val durations = listOf(
        1_800 to "30 min",
        3_600 to "1 hour",
        21_600 to "6 hours",
        86_400 to "1 day",
        259_200 to "3 days",
        604_800 to "7 days",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Poll, contentDescription = null, tint = palette.cyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("POLL", style = type.labelMedium, color = palette.label)
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Remove poll",
                tint = palette.textDim,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onRemove),
            )
        }
        Spacer(Modifier.height(12.dp))
        poll.options.forEachIndexed { index, option ->
            Row(
                modifier = Modifier.padding(bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val fieldShape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(fieldShape)
                        .background(palette.bg.copy(alpha = .4f))
                        .border(1.dp, palette.border, fieldShape)
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                ) {
                    BasicTextField(
                        value = option,
                        onValueChange = { value ->
                            onUpdate { it.copy(options = it.options.toMutableList().apply { this[index] = value }) }
                        },
                        singleLine = true,
                        textStyle = type.bodyMedium.copy(color = palette.text, fontWeight = FontWeight.SemiBold),
                        cursorBrush = SolidColor(palette.cyan),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (option.isEmpty()) {
                                Text("Option ${index + 1}", style = type.bodyMedium, color = palette.textMute)
                            }
                            inner()
                        },
                    )
                }
                if (poll.options.size > 2) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Rounded.RemoveCircleOutline,
                        contentDescription = "Remove option",
                        tint = palette.textMute,
                        modifier = Modifier
                            .size(19.dp)
                            .clickable {
                                onUpdate { it.copy(options = it.options.toMutableList().apply { removeAt(index) }) }
                            },
                    )
                }
            }
        }
        if (poll.options.size < 4) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    onUpdate { it.copy(options = it.options + "") }
                },
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = palette.cyan, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    "Add option",
                    style = type.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.cyan,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val fieldShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(fieldShape)
                    .background(palette.bg.copy(alpha = .4f))
                    .border(1.dp, palette.border, fieldShape)
                    .clickable { durationMenuOpen = true }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
            ) {
                Text(
                    "Ends in ${durations.firstOrNull { it.first == poll.expiresInSeconds }?.second ?: "1 day"}",
                    style = type.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.text,
                )
                DropdownMenu(expanded = durationMenuOpen, onDismissRequest = { durationMenuOpen = false }) {
                    durations.forEach { (seconds, label) ->
                        DropdownMenuItem(
                            text = { Text("Ends in $label", style = type.bodySmall, color = palette.text) },
                            onClick = {
                                durationMenuOpen = false
                                onUpdate { it.copy(expiresInSeconds = seconds) }
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            val multiple = poll.multiple
            Box(
                modifier = Modifier
                    .clip(fieldShape)
                    .background(if (multiple) palette.cyan.copy(alpha = .08f) else palette.bg.copy(alpha = .4f))
                    .border(1.dp, if (multiple) palette.cyan.copy(alpha = .4f) else palette.border, fieldShape)
                    .clickable { onUpdate { it.copy(multiple = !it.multiple) } }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
            ) {
                Text(
                    "Multiple",
                    style = type.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (multiple) palette.cyan else palette.textDim,
                )
            }
        }
    }
}

/** Visibility selector pill for the composer; opens a bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisibilityPicker(value: String, onChanged: (String) -> Unit) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    var sheetOpen by remember { mutableStateOf(false) }

    val options: List<Triple<String, ImageVector, String>> = listOf(
        Triple("public", Icons.Rounded.Public, "Public"),
        Triple("unlisted", Icons.Rounded.LockOpen, "Unlisted"),
        Triple("private", Icons.Rounded.Lock, "Followers"),
        Triple("direct", Icons.Rounded.AlternateEmail, "Direct"),
    )
    val current = options.firstOrNull { it.first == value } ?: options[0]
    val shape = RoundedCornerShape(11.dp)

    Row(
        modifier = Modifier
            .clip(shape)
            .background(palette.cyan.copy(alpha = .08f))
            .border(1.dp, palette.cyan.copy(alpha = .3f), shape)
            .clickable { sheetOpen = true }
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(current.second, contentDescription = null, tint = palette.cyan, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            current.third,
            style = type.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = palette.cyan,
        )
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            containerColor = palette.surfaceSolid,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                options.forEach { (key, icon, label) ->
                    val selected = key == value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                sheetOpen = false
                                onChanged(key)
                            }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (selected) palette.cyan else palette.textDim,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            label,
                            style = type.titleSmall,
                            color = if (selected) palette.cyan else palette.text,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = palette.cyan,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
