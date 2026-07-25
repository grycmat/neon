package com.gigapingu.neon.feature.profile

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AccountRepository
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.model.AccountField
import com.gigapingu.neon.core.network.FilePart
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val MAX_ACCOUNT_FIELDS = 4

data class EditProfileUiState(
    val displayName: String = "",
    val bio: String = "",
    val locked: Boolean = false,
    val fields: List<AccountField> = emptyList(),
    val avatarName: String? = null,
    val headerName: String? = null,
    val saving: Boolean = false,
    val done: Boolean = false,
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val application: Application,
    private val accounts: AccountRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    val me get() = auth.me.value

    private var avatarUri: Uri? = null
    private var headerUri: Uri? = null
    private var initialized = false

    fun start(plainBio: String, initialFields: List<AccountField> = emptyList()) {
        if (initialized) return
        initialized = true
        val account = me ?: return
        _uiState.update {
            it.copy(displayName = account.displayName, bio = plainBio, locked = account.locked, fields = initialFields)
        }
    }

    fun onDisplayNameChange(value: String) = _uiState.update { it.copy(displayName = value) }
    fun onBioChange(value: String) = _uiState.update { it.copy(bio = value) }
    fun onLockedChange(value: Boolean) = _uiState.update { it.copy(locked = value) }

    fun onFieldNameChange(index: Int, value: String) = _uiState.update { state ->
        state.copy(fields = state.fields.mapIndexed { i, f -> if (i == index) f.copy(name = value) else f })
    }

    fun onFieldValueChange(index: Int, value: String) = _uiState.update { state ->
        state.copy(fields = state.fields.mapIndexed { i, f -> if (i == index) f.copy(value = value) else f })
    }

    fun addField() = _uiState.update { state ->
        if (state.fields.size >= MAX_ACCOUNT_FIELDS) state else state.copy(fields = state.fields + AccountField())
    }

    fun removeField(index: Int) = _uiState.update { state ->
        state.copy(fields = state.fields.filterIndexed { i, _ -> i != index })
    }

    fun onAvatarPicked(uri: Uri?) {
        avatarUri = uri ?: return
        _uiState.update { it.copy(avatarName = uri.lastPathSegment ?: "avatar") }
    }

    fun onHeaderPicked(uri: Uri?) {
        headerUri = uri ?: return
        _uiState.update { it.copy(headerName = uri.lastPathSegment ?: "header") }
    }

    fun save() {
        val state = _uiState.value
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val avatar = avatarUri?.let { readPart("avatar", it) }
                val header = headerUri?.let { readPart("header", it) }
                // Pad to MAX_ACCOUNT_FIELDS with blanks so a removed field is cleared
                // server-side too — Mastodon replaces the whole fields_attributes set.
                val paddedFields = state.fields + List(MAX_ACCOUNT_FIELDS - state.fields.size) { AccountField() }
                val updated = accounts.updateCredentials(
                    displayName = state.displayName,
                    note = state.bio,
                    locked = state.locked,
                    fields = paddedFields,
                    avatar = avatar,
                    header = header,
                )
                auth.updateMe(updated)
                _uiState.update { it.copy(done = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(saving = false) }
                _errors.tryEmit("Save failed: ${e.message}")
            }
        }
    }

    private suspend fun readPart(name: String, uri: Uri): FilePart = withContext(Dispatchers.IO) {
        val resolver = application.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read image")
        var filename = "$name.jpg"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.let { filename = it }
        }
        FilePart(name = name, filename = filename, bytes = bytes)
    }
}
