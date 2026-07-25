package com.gigapingu.neon.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.model.MastodonList
import com.gigapingu.neon.core.ui.AsyncList
import com.gigapingu.neon.core.ui.Navigator

/** Settings > Lists — create/rename/delete lists, tap to open its timeline. */
@Composable
fun ManageListsScreen(viewModel: ListsViewModel = hiltViewModel()) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editorTarget by remember { mutableStateOf<MastodonList?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MastodonList?>(null) }

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
            Text("Lists", style = type.headlineMedium, color = palette.text, modifier = Modifier.weight(1f))
            GlassIconButton(
                icon = Icons.Rounded.Add,
                onClick = { showCreate = true },
                contentDescription = "New list",
            )
        }
        AsyncList(
            state = state,
            onRefresh = viewModel::refresh,
            emptyLabel = "You haven't made any lists yet",
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
            key = { it.id },
        ) { list ->
            ListRow(
                list = list,
                onClick = { Navigator.openListTimeline(list.id, list.title) },
                onRename = { editorTarget = list },
                onDelete = { deleteTarget = list },
            )
        }
    }

    if (showCreate) {
        ListEditorDialog(
            initialTitle = "",
            title = "New list",
            onConfirm = {
                viewModel.create(it)
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }
    editorTarget?.let { list ->
        ListEditorDialog(
            initialTitle = list.title,
            title = "Rename list",
            onConfirm = {
                viewModel.rename(list, it)
                editorTarget = null
            },
            onDismiss = { editorTarget = null },
        )
    }
    deleteTarget?.let { list ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete \"${list.title}\"?", color = palette.text) },
            text = { Text("This can't be undone.", color = palette.textDim, style = type.bodySmall) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(list); deleteTarget = null }) {
                    Text("Delete", color = palette.pink)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = palette.textMute)
                }
            },
            containerColor = palette.surfaceSolid,
            shape = RoundedCornerShape(20.dp),
        )
    }
}

@Composable
private fun ListRow(
    list: MastodonList,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(list.title, style = type.titleSmall, color = palette.text, modifier = Modifier.weight(1f))
            GlassIconButton(icon = Icons.Rounded.Edit, onClick = onRename, contentDescription = "Rename \"${list.title}\"")
            Spacer(Modifier.width(8.dp))
            GlassIconButton(icon = Icons.Rounded.Delete, onClick = onDelete, contentDescription = "Delete \"${list.title}\"")
        }
    }
}

@Composable
private fun ListEditorDialog(
    initialTitle: String,
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    var text by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = palette.text) },
        text = {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = type.bodyMedium.copy(color = palette.text),
                cursorBrush = SolidColor(palette.cyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface, RoundedCornerShape(8.dp))
                    .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text("List name", style = type.bodyMedium, color = palette.textMute)
                    }
                    inner()
                },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("Save", color = palette.cyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.textMute)
            }
        },
        containerColor = palette.surfaceSolid,
        shape = RoundedCornerShape(20.dp),
    )
}
