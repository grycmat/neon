package com.gigapingu.neon.core.ui

import android.app.Application
import android.content.Intent
import android.widget.Toast
import com.gigapingu.neon.core.data.AccountRepository
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.data.SearchRepository
import com.gigapingu.neon.core.data.StatusRepository
import com.gigapingu.neon.core.model.Account
import com.gigapingu.neon.core.model.Poll
import com.gigapingu.neon.core.model.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Status interactions (favourite / boost / vote / share / mention taps),
 * callable directly from any composable. NeonApplication wires the
 * repositories in onCreate; until then every call is a no-op (previews).
 */
object StatusActionService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var app: Application? = null
    private var statuses: StatusRepository? = null
    private var search: SearchRepository? = null
    private var auth: AuthRepository? = null
    private var accounts: AccountRepository? = null

    fun init(
        app: Application,
        statuses: StatusRepository,
        search: SearchRepository,
        auth: AuthRepository,
        accounts: AccountRepository,
    ) {
        this.app = app
        this.statuses = statuses
        this.search = search
        this.auth = auth
        this.accounts = accounts
    }

    fun isOwnStatus(status: Status): Boolean {
        return status.display.account.id == auth?.me?.value?.id
    }

    fun deleteStatus(status: Status) = guarded {
        statuses?.delete(status.id)
    }

    fun editStatus(status: Status) {
        Navigator.openCompose(editStatusId = status.id)
    }

    fun redraftStatus(status: Status) = guarded {
        val source = statuses?.getSource(status.id) ?: return@guarded
        statuses?.delete(status.id)
        Navigator.openCompose(
            redraftText = source.text,
            redraftSpoilerText = source.spoilerText,
            redraftVisibility = status.visibility,
        )
    }

    fun muteAccount(account: Account) = guarded {
        accounts?.mute(account.id)
        app?.let { Toast.makeText(it, "Muted @${account.acct}", Toast.LENGTH_SHORT).show() }
    }

    fun blockAccount(account: Account) = guarded {
        accounts?.block(account.id)
        app?.let { Toast.makeText(it, "Blocked @${account.acct}", Toast.LENGTH_SHORT).show() }
    }

    fun reportStatus(status: Status, comment: String? = null) = guarded {
        accounts?.report(accountId = status.account.id, statusId = status.id, comment = comment)
        app?.let { Toast.makeText(it, "Reported status", Toast.LENGTH_SHORT).show() }
    }

    fun toggleFavourite(status: Status) = guarded { statuses?.favourite(status) }

    fun toggleBoost(status: Status) = guarded { statuses?.reblog(status) }

    fun toggleBookmark(status: Status) = guarded { statuses?.bookmark(status) }

    fun vote(poll: Poll, choices: List<Int>) = guarded { statuses?.vote(poll, choices) }

    fun share(status: Status) {
        val app = app ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, status.shareUrl)
        }
        app.startActivity(
            Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Resolves a tapped mention via the status' mention list + account search. */
    fun openMention(status: Status, acctOrUrl: String) {
        val search = search ?: return
        val mention = status.mentions.firstOrNull {
            it.url == acctOrUrl || "@${it.username}" == acctOrUrl || "@${it.acct}" == acctOrUrl
        } ?: return
        scope.launch {
            runCatching { search.searchAccounts(mention.acct, limit = 1) }
                .onSuccess { found ->
                    found.firstOrNull()?.let { Navigator.openProfile(it.id) }
                }
        }
    }

    private fun guarded(action: suspend () -> Unit) {
        scope.launch {
            try {
                action()
            } catch (e: Exception) {
                app?.let {
                    Toast.makeText(it, e.message ?: "Something went wrong", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}
