package com.gigapingu.neon

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigapingu.neon.core.data.AccountRepository
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.data.AuthStatus
import com.gigapingu.neon.core.data.SearchRepository
import com.gigapingu.neon.core.data.SettingsRepository
import com.gigapingu.neon.core.data.StatusRepository
import com.gigapingu.neon.core.data.ThemeMode
import com.gigapingu.neon.core.model.Account
import com.gigapingu.neon.core.model.Poll
import com.gigapingu.neon.core.model.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Mention taps resolve to an account id the UI should navigate to. */
sealed interface ShellEvent {
    data class OpenProfile(val accountId: String) : ShellEvent
    data class Message(val text: String) : ShellEvent
}

/**
 * App-level state: auth gate, theme, and the status interaction handler
 * backing every StatusActions row (favourite/boost/vote/share/mentions).
 */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val application: Application,
    private val auth: AuthRepository,
    settings: SettingsRepository,
    private val statuses: StatusRepository,
    private val search: SearchRepository,
    private val accounts: AccountRepository,
) : ViewModel() {

    val authStatus: StateFlow<AuthStatus> = auth.status
    val me: StateFlow<Account?> = auth.me

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.Dark)

    private val _events = MutableSharedFlow<ShellEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ShellEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch { auth.restore() }
    }

    fun toggleFavourite(status: Status) = guarded { statuses.favourite(status) }

    fun toggleBoost(status: Status) = guarded { statuses.reblog(status) }

    fun vote(poll: Poll, choices: List<Int>) = guarded { statuses.vote(poll, choices) }

    fun share(status: Status) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, status.shareUrl)
        }
        application.startActivity(
            Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Resolves a tapped mention via the status' mention list + account search. */
    fun openMention(status: Status, acctOrUrl: String) {
        val mention = status.mentions.firstOrNull {
            it.url == acctOrUrl || "@${it.username}" == acctOrUrl || "@${it.acct}" == acctOrUrl
        } ?: return
        viewModelScope.launch {
            runCatching { search.searchAccounts(mention.acct, limit = 1) }
                .onSuccess { found ->
                    found.firstOrNull()?.let { _events.tryEmit(ShellEvent.OpenProfile(it.id)) }
                }
        }
    }

    private fun guarded(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (e: Exception) {
                _events.tryEmit(ShellEvent.Message(e.message ?: "Something went wrong"))
            }
        }
    }
}
