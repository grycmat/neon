package com.gigapingu.neon.core.ui

import android.Manifest
import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.gigapingu.neon.core.data.AccountRepository
import com.gigapingu.neon.core.data.AuthRepository
import com.gigapingu.neon.core.data.SearchRepository
import com.gigapingu.neon.core.data.StatusRepository
import com.gigapingu.neon.core.model.Account
import com.gigapingu.neon.core.model.MediaAttachment
import com.gigapingu.neon.core.model.Poll
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.model.StatusEdit
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

    fun reportAccount(account: Account, comment: String? = null) = guarded {
        accounts?.report(accountId = account.id, comment = comment)
        app?.let { Toast.makeText(it, "Reported @${account.acct}", Toast.LENGTH_SHORT).show() }
    }

    fun toggleFavourite(status: Status) = guarded { statuses?.favourite(status) }

    fun toggleBoost(status: Status) = guarded { statuses?.reblog(status) }

    fun toggleBookmark(status: Status) = guarded { statuses?.bookmark(status) }

    fun togglePin(status: Status) = guarded { statuses?.pin(status) }

    suspend fun getFavouritedBy(statusId: String): Result<List<Account>> =
        runCatching { accounts?.getFavouritedBy(statusId) ?: emptyList() }

    suspend fun getHistory(statusId: String): Result<List<StatusEdit>> =
        runCatching { statuses?.getHistory(statusId) ?: emptyList() }

    suspend fun getRebloggedBy(statusId: String): Result<List<Account>> =
        runCatching { accounts?.getRebloggedBy(statusId) ?: emptyList() }

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

    /** Resolves a tapped mention with no status context (e.g. an account bio) by acct/URL alone. */
    fun openMention(acctOrUrl: String) {
        val search = search ?: return
        val handle = acctOrUrl.substringAfterLast('/').removePrefix("@")
        if (handle.isBlank()) return
        scope.launch {
            runCatching { search.searchAccounts(handle, limit = 1) }
                .onSuccess { found ->
                    found.firstOrNull()?.let { Navigator.openProfile(it.id) }
                }
        }
    }

    /**
     * Hands the attachment URL to [DownloadManager] so it saves into the public
     * Pictures/Movies dir with a system progress notification — no OkHttp/fetch code needed here,
     * and no permission required from API 29 on, since DownloadManager writes through its own
     * process rather than this app's sandbox. Pre-29 it still needs WRITE_EXTERNAL_STORAGE
     * (declared maxSdkVersion="28" in the manifest); rather than add a second Activity-scoped
     * runtime-permission launcher solely for that now-vanishingly rare OS range, a missing grant
     * just routes the user to this app's settings page to flip it on and retry.
     */
    fun saveMedia(attachment: MediaAttachment) {
        val app = app ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(app, "Enable storage permission in Settings to save media", Toast.LENGTH_LONG).show()
            runCatching {
                app.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            return
        }
        runCatching {
            val fileName = attachment.url.substringAfterLast('/').substringBefore('?')
                .ifBlank { "neon_${System.currentTimeMillis()}" }
            val dir = if (attachment.isPlayable) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
            val request = DownloadManager.Request(Uri.parse(attachment.url))
                .setTitle(fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(dir, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            val downloadManager = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(app, "Saving media…", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(app, "Couldn't save media", Toast.LENGTH_SHORT).show()
        }
    }

    fun openUrl(url: String) {
        val app = app ?: return
        runCatching {
            app.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            Toast.makeText(app, "Couldn't open link", Toast.LENGTH_SHORT).show()
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
