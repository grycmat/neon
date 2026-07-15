package com.gigapingu.neon.feature.composer

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.MediaRepository
import com.gigapingu.neon.core.data.SearchRepository
import com.gigapingu.neon.core.data.StatusRepository
import com.gigapingu.neon.core.model.Account
import com.gigapingu.neon.core.model.MediaAttachment
import com.gigapingu.neon.core.model.PollDraft
import com.gigapingu.neon.core.model.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val MAX_CHARS = 500
const val MAX_MEDIA = 4

data class PollDraftState(
    val options: List<String> = listOf("", ""),
    val expiresInSeconds: Int = 86_400,
    val multiple: Boolean = false,
)

data class ComposeUiState(
    val title: String = "New toot",
    val replyTo: Status? = null,
    val quoting: Status? = null,
    val text: String = "",
    val visibility: String = "public",
    val showCw: Boolean = false,
    val cwText: String = "",
    val media: List<MediaAttachment> = emptyList(),
    val uploading: Boolean = false,
    val poll: PollDraftState? = null,
    val suggestions: List<Account> = emptyList(),
    val posting: Boolean = false,
    val done: Boolean = false,
) {
    val canPost: Boolean
        get() = !posting && !uploading && text.isNotBlank() && text.length <= MAX_CHARS
}

@OptIn(FlowPreview::class)
@HiltViewModel
class ComposeViewModel @Inject constructor(
    private val application: Application,
    private val statuses: StatusRepository,
    private val media: MediaRepository,
    private val search: SearchRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComposeUiState())
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val mentionToken = MutableStateFlow<String?>(null)
    private var initialized = false

    init {
        viewModelScope.launch {
            mentionToken.debounce(300).collectLatest { token ->
                if (token == null || token.length < 2) {
                    _uiState.update { it.copy(suggestions = emptyList()) }
                } else {
                    runCatching { search.searchAccounts(token) }
                        .onSuccess { accounts -> _uiState.update { it.copy(suggestions = accounts) } }
                }
            }
        }
    }

    /** Loads reply/quote context; prefills @-handles when replying. */
    fun start(replyToId: String?, quotingId: String?) {
        if (initialized) return
        initialized = true
        if (replyToId == null && quotingId == null) return
        viewModelScope.launch {
            try {
                if (replyToId != null) {
                    val reply = statuses.getStatus(replyToId)
                    val handles = buildSet {
                        add("@${reply.account.acct}")
                        reply.mentions.forEach { add("@${it.acct}") }
                    }
                    _uiState.update {
                        it.copy(
                            title = "Reply",
                            replyTo = reply,
                            text = handles.joinToString(" ") + " ",
                            visibility = reply.visibility,
                        )
                    }
                }
                if (quotingId != null) {
                    val quoting = statuses.getStatus(quotingId)
                    _uiState.update { it.copy(title = "Quote", quoting = quoting) }
                }
            } catch (e: Exception) {
                _errors.tryEmit("Could not load context: ${e.message}")
            }
        }
    }

    // ── Text + mention autocomplete ──

    /** [caret] is the cursor position, used to find the @word being typed. */
    fun onTextChange(text: String, caret: Int) {
        _uiState.update { it.copy(text = text) }
        val before = text.take(caret.coerceIn(0, text.length))
        mentionToken.value = Regex("@([\\w.@-]+)$").find(before)?.groupValues?.get(1)
    }

    /** Replaces the @token before [caret] with the picked account; returns the new text + caret. */
    fun applyMention(account: Account, caret: Int): Pair<String, Int> {
        val text = _uiState.value.text
        val before = text.take(caret.coerceIn(0, text.length))
        val after = text.drop(caret.coerceIn(0, text.length))
        val replaced = before.replace(Regex("@[\\w.@-]+$"), "@${account.acct} ")
        val newText = replaced + after
        _uiState.update { it.copy(text = newText, suggestions = emptyList()) }
        return newText to replaced.length
    }

    fun setVisibility(visibility: String) = _uiState.update { it.copy(visibility = visibility) }

    fun toggleCw() = _uiState.update { it.copy(showCw = !it.showCw) }

    fun onCwChange(text: String) = _uiState.update { it.copy(cwText = text) }

    // ── Media ──

    fun pickMedia(uris: List<Uri>) {
        val state = _uiState.value
        if (uris.isEmpty() || state.poll != null || state.media.size >= MAX_MEDIA) return
        _uiState.update { it.copy(uploading = true) }
        viewModelScope.launch {
            try {
                uris.take(MAX_MEDIA - state.media.size).forEach { uri ->
                    val (bytes, name) = withContext(Dispatchers.IO) { readUri(uri) }
                    val uploaded = media.upload(bytes, filename = name)
                    _uiState.update { it.copy(media = it.media + uploaded) }
                }
            } catch (e: Exception) {
                _errors.tryEmit("Upload failed: ${e.message}")
            } finally {
                _uiState.update { it.copy(uploading = false) }
            }
        }
    }

    fun removeMedia(attachment: MediaAttachment) {
        _uiState.update { state -> state.copy(media = state.media.filterNot { it.id == attachment.id }) }
    }

    fun updateAlt(attachment: MediaAttachment, description: String) {
        viewModelScope.launch {
            runCatching { media.updateDescription(attachment.id, description) }
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(media = state.media.map { if (it.id == updated.id) updated else it })
                    }
                }
        }
    }

    private fun readUri(uri: Uri): Pair<ByteArray, String> {
        val resolver = application.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read file")
        var name = "media.jpg"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)?.let { name = it }
            }
        }
        return bytes to name
    }

    // ── Poll ──

    fun togglePoll() {
        _uiState.update { state ->
            when {
                state.poll != null -> state.copy(poll = null)
                state.media.isNotEmpty() -> state // polls exclude media
                else -> state.copy(poll = PollDraftState())
            }
        }
    }

    fun updatePoll(transform: (PollDraftState) -> PollDraftState) {
        _uiState.update { state -> state.copy(poll = state.poll?.let(transform)) }
    }

    // ── Post ──

    fun post() {
        val state = _uiState.value
        if (!state.canPost) return
        _uiState.update { it.copy(posting = true) }
        viewModelScope.launch {
            try {
                val draft = state.poll?.let { poll ->
                    val filled = poll.options.map(String::trim).filter(String::isNotEmpty)
                    if (filled.size >= 2) {
                        PollDraft(filled, poll.expiresInSeconds, poll.multiple)
                    } else {
                        null
                    }
                }
                statuses.create(
                    text = state.text.trim(),
                    visibility = state.visibility,
                    inReplyToId = state.replyTo?.id,
                    quotedStatusId = state.quoting?.id,
                    spoilerText = if (state.showCw) state.cwText.trim() else null,
                    mediaIds = state.media.map { it.id },
                    poll = draft,
                )
                _uiState.update { it.copy(done = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(posting = false) }
                _errors.tryEmit("Post failed: ${e.message}")
            }
        }
    }
}
