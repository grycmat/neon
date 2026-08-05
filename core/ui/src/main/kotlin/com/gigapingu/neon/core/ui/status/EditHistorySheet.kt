package com.gigapingu.neon.core.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.designsystem.component.HtmlText
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.relativeTime
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.model.StatusEdit
import com.gigapingu.neon.core.ui.ErrorPane
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.StatusActionService

/** Prior revisions of an edited status — text + media per version, oldest first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHistorySheet(statusId: String, status: Status, onDismiss: () -> Unit) {
    val palette = NeonTheme.palette
    var history by remember { mutableStateOf<List<StatusEdit>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(statusId) {
        StatusActionService.getHistory(statusId)
            .onSuccess { history = it }
            .onFailure { error = it.message ?: "Could not load edit history" }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceSolid,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Text(
                "Edit history",
                style = NeonTheme.type.titleMedium,
                color = palette.text,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            when {
                error != null -> ErrorPane(message = error!!, modifier = Modifier.height(220.dp))
                history == null -> Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = palette.cyan)
                }
                history!!.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No edit history", style = NeonTheme.type.bodyMedium, color = palette.textMute)
                }
                else -> LazyColumn(modifier = Modifier.heightIn(max = 460.dp)) {
                    items(history!!, key = { it.createdAt?.toEpochMilli() ?: it.hashCode() }) { edit ->
                        EditHistoryRow(edit, status)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditHistoryRow(edit: StatusEdit, status: Status) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(relativeTime(edit.createdAt), style = type.bodySmall, color = palette.textMute)
        Spacer(Modifier.height(4.dp))
        if (edit.spoilerText.isNotEmpty()) {
            Text(
                edit.spoilerText,
                style = type.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = palette.purple,
            )
            Spacer(Modifier.height(4.dp))
        }
        HtmlText(
            edit.content,
            style = type.bodyMedium,
            emojis = edit.emojis,
            onHashtagClick = { tag -> Navigator.openHashtag(tag) },
            onMentionClick = { acctOrUrl -> StatusActionService.openMention(status, acctOrUrl) },
            onLinkClick = { url -> StatusActionService.openUrl(url) },
        )
        if (edit.mediaAttachments.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            MediaGrid(attachments = edit.mediaAttachments, sensitive = edit.sensitive)
        }
        Box(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider),
        )
    }
}
