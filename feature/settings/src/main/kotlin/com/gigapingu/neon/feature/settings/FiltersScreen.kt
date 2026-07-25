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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.model.FilterContext
import com.gigapingu.neon.core.model.ServerFilter
import com.gigapingu.neon.core.ui.AsyncList
import com.gigapingu.neon.core.ui.Navigator

private val ExpiryOptions = listOf(
    "Never" to null,
    "30 minutes" to 1_800,
    "1 hour" to 3_600,
    "6 hours" to 21_600,
    "1 day" to 86_400,
    "1 week" to 604_800,
)

/** Settings > Filters — create/edit/delete keyword filters. */
@Composable
fun FiltersScreen(viewModel: FiltersViewModel = hiltViewModel()) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editorTarget by remember { mutableStateOf<ServerFilter?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ServerFilter?>(null) }

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
            Text("Filters", style = type.headlineMedium, color = palette.text, modifier = Modifier.weight(1f))
            GlassIconButton(
                icon = Icons.Rounded.Add,
                onClick = { showCreate = true },
                contentDescription = "New filter",
            )
        }
        AsyncList(
            state = state,
            onRefresh = viewModel::refresh,
            emptyLabel = "No filters yet — muted words and phrases will show up here",
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
            key = { it.id },
        ) { filter ->
            FilterRow(
                filter = filter,
                onEdit = { editorTarget = filter },
                onDelete = { deleteTarget = filter },
            )
        }
    }

    if (showCreate) {
        FilterEditorSheet(
            editing = null,
            onSave = { title, phrase, contexts, wholeWord, action, expires ->
                viewModel.save(null, title, phrase, contexts, wholeWord, action, expires)
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }
    editorTarget?.let { filter ->
        FilterEditorSheet(
            editing = filter,
            onSave = { title, phrase, contexts, wholeWord, action, expires ->
                viewModel.save(filter, title, phrase, contexts, wholeWord, action, expires)
                editorTarget = null
            },
            onDismiss = { editorTarget = null },
        )
    }
    deleteTarget?.let { filter ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete \"${filter.title}\"?", color = palette.text) },
            text = { Text("This can't be undone.", color = palette.textDim, style = type.bodySmall) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(filter); deleteTarget = null }) {
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
private fun FilterRow(filter: ServerFilter, onEdit: () -> Unit, onDelete: () -> Unit) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(filter.title.ifEmpty { filter.phrase }, style = type.titleSmall, color = palette.text)
                Text(
                    filter.context.joinToString(", ") + if (filter.filterAction == "hide") " · hide" else " · warn",
                    style = type.bodySmall,
                    color = palette.textDim,
                )
            }
            GlassIconButton(icon = Icons.Rounded.Edit, onClick = onEdit, contentDescription = "Edit \"${filter.title}\"")
            Spacer(Modifier.width(8.dp))
            GlassIconButton(icon = Icons.Rounded.Delete, onClick = onDelete, contentDescription = "Delete \"${filter.title}\"")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterEditorSheet(
    editing: ServerFilter?,
    onSave: (title: String, phrase: String, contexts: List<String>, wholeWord: Boolean, action: String, expiresIn: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    var title by remember { mutableStateOf(editing?.title.orEmpty()) }
    var phrase by remember { mutableStateOf(editing?.phrase.orEmpty()) }
    var contexts by remember { mutableStateOf(editing?.context?.toSet() ?: setOf(FilterContext.Home, FilterContext.Public)) }
    var wholeWord by remember { mutableStateOf(editing?.wholeWord ?: true) }
    var hideCompletely by remember { mutableStateOf(editing?.filterAction == "hide") }
    var expiresIn by remember { mutableStateOf<Int?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceSolid,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Text(
                if (editing != null) "Edit filter" else "New filter",
                style = type.titleMedium,
                color = palette.text,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            LabeledField(label = "Title (optional)", value = title, onValueChange = { title = it })
            Spacer(Modifier.height(10.dp))
            LabeledField(label = "Keyword or phrase", value = phrase, onValueChange = { phrase = it })
            Spacer(Modifier.height(16.dp))
            Text("Apply in", style = type.bodySmall.copy(fontWeight = FontWeight.Bold), color = palette.textDim)
            Spacer(Modifier.height(8.dp))
            Column {
                listOf(
                    FilterContext.Home to "Home and lists",
                    FilterContext.Notifications to "Notifications",
                    FilterContext.Public to "Public timelines",
                    FilterContext.Thread to "Conversations",
                    FilterContext.Account to "Profiles",
                ).forEach { (ctx, label) ->
                    ContextRow(
                        label = label,
                        checked = ctx in contexts,
                        onToggle = { contexts = if (ctx in contexts) contexts - ctx else contexts + ctx },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            ToggleRow(
                label = "Whole word only",
                checked = wholeWord,
                onCheckedChange = { wholeWord = it },
            )
            ToggleRow(
                label = "Hide completely instead of warning",
                checked = hideCompletely,
                onCheckedChange = { hideCompletely = it },
            )
            Spacer(Modifier.height(12.dp))
            Text("Expire after", style = type.bodySmall.copy(fontWeight = FontWeight.Bold), color = palette.textDim)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExpiryOptions.forEach { (label, seconds) ->
                    val selected = expiresIn == seconds
                    val shape = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .background(if (selected) palette.cyan.copy(alpha = .12f) else palette.surface, shape)
                            .border(1.dp, if (selected) palette.cyan.copy(alpha = .4f) else palette.border, shape)
                            .clickable { expiresIn = seconds }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(label, style = type.labelSmall, color = if (selected) palette.cyan else palette.textDim)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel", color = palette.textMute)
                }
                TextButton(
                    onClick = {
                        onSave(
                            title,
                            phrase,
                            contexts.toList(),
                            wholeWord,
                            if (hideCompletely) "hide" else "warn",
                            expiresIn,
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save", color = palette.cyan)
                }
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, onValueChange: (String) -> Unit) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    Column {
        Text(label, style = type.bodySmall, color = palette.textDim)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = type.bodyMedium.copy(color = palette.text),
            cursorBrush = SolidColor(palette.cyan),
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface, RoundedCornerShape(8.dp))
                .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                .padding(10.dp),
        )
    }
}

@Composable
private fun ContextRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = RoundedCornerShape(5.dp)
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(if (checked) palette.cyan.copy(alpha = .16f) else palette.surface, shape)
                .border(1.dp, if (checked) palette.cyan else palette.border, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = palette.cyan, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(label, style = type.bodyMedium, color = palette.text)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
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
