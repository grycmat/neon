package com.gigapingu.neon.feature.composer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Poll
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassField
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.GradientButton
import com.gigapingu.neon.core.designsystem.component.NeonAvatar
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.htmlToPlainText
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.PreviewFixtures
import com.gigapingu.neon.core.ui.PreviewHarness
import com.gigapingu.neon.core.ui.isBigScreen
import com.gigapingu.neon.core.ui.status.QuoteCard

/**
 * Composer: new toot, reply, or quote. Media attachments (with alt text),
 * polls, content warnings, visibility, @-mention autocomplete, # tags.
 */
@Composable
fun ComposeScreen(
    replyToId: String? = null,
    quotingId: String? = null,
    editStatusId: String? = null,
    redraftText: String? = null,
    redraftSpoilerText: String? = null,
    redraftVisibility: String? = null,
    viewModel: ComposeViewModel = hiltViewModel(),
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var textField by remember { mutableStateOf(TextFieldValue("")) }
    val bodyFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        bodyFocusRequester.requestFocus()
        keyboardController?.show()
    }
    LaunchedEffect(Unit) {
        viewModel.start(
            replyToId = replyToId,
            quotingId = quotingId,
            editStatusId = editStatusId,
            redraftText = redraftText,
            redraftSpoilerText = redraftSpoilerText,
            redraftVisibility = redraftVisibility,
        )
    }
    LaunchedEffect(Unit) { viewModel.errors.collect { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(uiState.done) { if (uiState.done) Navigator.back() }
    // Prefill from the ViewModel (reply handles or edit/redraft source arrive async/sync).
    LaunchedEffect(uiState.text) {
        if (textField.text.isEmpty() && uiState.text.isNotEmpty()) {
            textField = TextFieldValue(uiState.text, selection = TextRange(uiState.text.length))
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_MEDIA),
    ) { uris -> viewModel.pickMedia(uris) }

    NeonBackground {
        // Everything between the header row and the toolbar is shared between
        // the phone full-screen sheet and the big-screen centered dialog.
        val body: @Composable (Modifier) -> Unit = { bodyModifier ->
        Column(bodyModifier) {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    onClick = Navigator::back,
                    contentDescription = "Close composer",
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    uiState.title,
                    style = type.headlineMedium,
                    color = palette.text,
                    modifier = Modifier.weight(1f),
                )
                VisibilityPicker(value = uiState.visibility, onChanged = viewModel::setVisibility)
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 12.dp),
            ) {
                uiState.replyTo?.let { ContextCard(label = "Replying to", status = it) }
                if (uiState.showCw) {
                    GlassField(label = "Content warning") {
                        BasicTextField(
                            value = uiState.cwText,
                            onValueChange = viewModel::onCwChange,
                            textStyle = type.bodyMedium.copy(
                                color = palette.text,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            cursorBrush = SolidColor(palette.cyan),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (uiState.cwText.isEmpty()) {
                                    Text(
                                        "Write your warning here",
                                        style = type.bodyMedium,
                                        color = palette.textMute,
                                    )
                                }
                                inner()
                            },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
                BasicTextField(
                    value = textField,
                    onValueChange = { value ->
                        textField = value
                        viewModel.onTextChange(value.text, value.selection.start)
                    },
                    textStyle = type.bodyLarge.copy(color = palette.text, fontSize = 16.sp),
                    cursorBrush = SolidColor(palette.cyan),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp)
                        .focusRequester(bodyFocusRequester),
                    decorationBox = { inner ->
                        if (textField.text.isEmpty()) {
                            Text(
                                "What's crackling?",
                                style = type.bodyLarge.copy(fontSize = 16.sp),
                                color = palette.textMute,
                            )
                        }
                        inner()
                    },
                )
                if (uiState.suggestions.isNotEmpty()) {
                    MentionSuggestions(
                        suggestions = uiState.suggestions,
                        onPick = { account ->
                            val (newText, caret) = viewModel.applyMention(account, textField.selection.start)
                            textField = TextFieldValue(newText, selection = TextRange(caret))
                        },
                    )
                }
                uiState.quoting?.let { QuoteCard(status = it) }
                Spacer(Modifier.height(12.dp))
                MediaStrip(
                    items = uiState.media,
                    onRemove = viewModel::removeMedia,
                    onEditAlt = viewModel::updateAlt,
                )
                if (uiState.uploading) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = palette.cyan,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Uploading media…", style = type.bodySmall, color = palette.textDim)
                    }
                }
                uiState.poll?.let { poll ->
                    Spacer(Modifier.height(8.dp))
                    PollEditor(
                        poll = poll,
                        onUpdate = viewModel::updatePoll,
                        onRemove = viewModel::togglePoll,
                    )
                }
            }

            // Toolbar + post button.
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.divider),
                )
                Row(
                    modifier = Modifier.padding(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIconButton(
                        icon = Icons.Outlined.Image,
                        tinted = uiState.media.isNotEmpty(),
                        onClick = if (uiState.poll == null) {
                            {
                                mediaPicker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            }
                        } else {
                            null
                        },
                        contentDescription = "Attach media",
                    )
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(
                        icon = Icons.Outlined.Poll,
                        tinted = uiState.poll != null,
                        onClick = if (uiState.media.isEmpty()) viewModel::togglePoll else null,
                        contentDescription = "Add poll",
                    )
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(
                        icon = Icons.Rounded.AlternateEmail,
                        onClick = { textField = textField.insertToken("@") { t, c -> viewModel.onTextChange(t, c) } },
                        contentDescription = "Mention",
                    )
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(
                        icon = Icons.Rounded.Tag,
                        onClick = { textField = textField.insertToken("#") { t, c -> viewModel.onTextChange(t, c) } },
                        contentDescription = "Hashtag",
                    )
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(
                        icon = Icons.Rounded.WarningAmber,
                        tinted = uiState.showCw,
                        onClick = viewModel::toggleCw,
                        contentDescription = "Content warning",
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${textField.text.length} / $MAX_CHARS",
                        style = type.bodySmall,
                        color = if (textField.text.length > MAX_CHARS) palette.pink else palette.textMute,
                    )
                    Spacer(Modifier.width(12.dp))
                    GradientButton(
                        label = "Post",
                        height = 44.dp,
                        busy = uiState.posting,
                        modifier = Modifier.width(96.dp),
                        onClick = if (uiState.canPost) viewModel::post else null,
                    )
                }
            }
        }
        }

        if (isBigScreen()) {
            // Centered dialog over the neon backdrop; tapping outside cancels.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = Navigator::back,
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
                contentAlignment = Alignment.Center,
            ) {
                val dialogShape = RoundedCornerShape(26.dp)
                Box(
                    modifier = Modifier
                        .width(620.dp)
                        .fillMaxHeight(.88f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { /* swallow taps so they don't dismiss */ }
                        .shadow(
                            elevation = 24.dp,
                            shape = dialogShape,
                            ambientColor = palette.shadow,
                            spotColor = palette.shadow,
                        )
                        .clip(dialogShape)
                        .background(palette.surfaceSolid)
                        .border(1.dp, palette.border, dialogShape),
                ) {
                    body(Modifier.fillMaxSize())
                }
            }
        } else {
            body(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding(),
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun TextFieldValue.insertToken(
    token: String,
    notify: (String, Int) -> Unit,
): TextFieldValue {
    val offset = selection.start.coerceIn(0, text.length)
    val before = text.take(offset)
    val needsSpace = before.isNotEmpty() && !before.endsWith(" ")
    val inserted = (if (needsSpace) " " else "") + token
    val newText = before + inserted + text.drop(offset)
    val caret = offset + inserted.length
    notify(newText, caret)
    return TextFieldValue(newText, selection = TextRange(caret))
}

@Composable
private fun MentionSuggestions(
    suggestions: List<com.gigapingu.neon.core.model.Account>,
    onPick: (com.gigapingu.neon.core.model.Account) -> Unit,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .padding(top = 4.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surfaceSolid)
            .border(1.dp, palette.border, shape),
    ) {
        suggestions.forEach { account ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(account) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeonAvatar(account = account, size = 30.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(account.displayNameOrUsername, style = type.titleSmall, color = palette.text)
                    Text(account.fullHandle, style = type.bodySmall, color = palette.textDim)
                }
            }
        }
    }
}

@Composable
private fun ContextCard(label: String, status: Status) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val preview = htmlToPlainText(status.content).replace('\n', ' ').trim()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .padding(bottom = 14.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape)
            .padding(13.dp),
    ) {
        NeonAvatar(account = status.account, size = 28.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "$label ${status.account.fullHandle}",
                style = type.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = palette.cyan,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                preview,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = type.bodySmall,
                color = palette.textDim,
            )
        }
    }
}

@Preview(name = "Reply context card", showBackground = true, heightDp = 140)
@Composable
private fun ContextCardPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            ContextCard(label = "Replying to", status = PreviewFixtures.status)
        }
    }
}

@Preview(name = "Mention suggestions", showBackground = true, heightDp = 200)
@Composable
private fun MentionSuggestionsPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            MentionSuggestions(
                suggestions = listOf(PreviewFixtures.account, PreviewFixtures.account2),
                onPick = {},
            )
        }
    }
}
