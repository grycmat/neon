package com.gigapingu.neon.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.data.AsyncState
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.model.Account
import com.gigapingu.neon.core.model.MediaAttachment
import com.gigapingu.neon.core.model.Poll
import com.gigapingu.neon.core.model.PollOption
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.ui.media.MediaPreviewScreen
import com.gigapingu.neon.core.ui.status.MediaGrid
import com.gigapingu.neon.core.ui.status.PollView
import com.gigapingu.neon.core.ui.status.QuoteCard
import com.gigapingu.neon.core.ui.status.StatusActions
import com.gigapingu.neon.core.ui.status.StatusBody
import com.gigapingu.neon.core.ui.status.StatusCard
import com.gigapingu.neon.core.ui.status.StatusListSkeleton
import java.time.Instant

/** Sample data + no-op locals so status components can be previewed in isolation. */
object PreviewFixtures {
    val account = Account(
        id = "1",
        username = "aurora",
        acct = "aurora@mastodon.social",
        displayName = "Aurora Vex",
        note = "<p>Synthwave gardener. She/her.</p>",
        followersCount = 1284,
        followingCount = 311,
        statusesCount = 2048,
    )

    val account2 = Account(
        id = "2",
        username = "moss",
        acct = "moss@fosstodon.org",
        displayName = "Moss",
        note = "<p>Terminal dweller. Ferns and functional programming.</p>",
        followersCount = 96,
        followingCount = 240,
        statusesCount = 512,
    )

    val status = Status(
        id = "100",
        account = account,
        content = "<p>Just planted a row of <a class=\"hashtag\" href=\"#\">#neon</a> tulips — " +
            "they hum quietly at dusk. cc <a class=\"mention\" href=\"#\">@moss</a></p>",
        createdAt = Instant.now().minusSeconds(60 * 47),
        repliesCount = 3,
        reblogsCount = 12,
        favouritesCount = 48,
    )

    val boostStatus = Status(
        id = "110",
        account = account2,
        reblog = status.copy(id = "111"),
    )

    val cwStatus = Status(
        id = "120",
        account = account2,
        content = "<p>The finale was wild — the garden was a simulation all along.</p>",
        spoilerText = "Spoilers: Neon Garden S2",
        createdAt = Instant.now().minusSeconds(3600 * 5),
        repliesCount = 7,
        favouritesCount = 21,
    )

    /** Empty URLs on purpose — previews render the gradient placeholder, no network. */
    fun attachment(id: String, type: String = "image") = MediaAttachment(
        id = id,
        rawType = type,
        url = "",
        previewUrl = "",
        description = "Preview placeholder",
    )

    val mediaStatus = Status(
        id = "130",
        account = account,
        content = "<p>Dusk over the greenhouse.</p>",
        createdAt = Instant.now().minusSeconds(90),
        mediaAttachments = listOf(attachment("m1"), attachment("m2", type = "video")),
        favouritesCount = 132,
        reblogsCount = 18,
        favourited = true,
    )

    val quoteStatus = Status(
        id = "140",
        account = account2,
        content = "<p>This is exactly the vibe.</p>",
        createdAt = Instant.now().minusSeconds(60 * 8),
        quote = status.copy(id = "141"),
    )

    val poll = Poll(
        id = "9",
        votesCount = 96,
        voted = true,
        expired = false,
        ownVotes = listOf(0),
        options = listOf(
            PollOption("Pink", 61),
            PollOption("Cyan", 35),
        ),
    )

    val openPoll = Poll(
        id = "10",
        votesCount = 12,
        expiresAt = Instant.now().plusSeconds(3600 * 22),
        options = listOf(
            PollOption("Space Grotesk"),
            PollOption("Manrope"),
            PollOption("Both, mixed"),
        ),
    )

}

// Navigator and StatusActionService no-op while uninitialized, so previews
// need no fakes — just the theme and background.
@Composable
fun PreviewHarness(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    NeonTheme(darkTheme = darkTheme) {
        NeonBackground(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Preview(name = "Status card", showBackground = true, heightDp = 420)
@Composable
private fun StatusCardPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            StatusCard(status = PreviewFixtures.status)
            StatusCard(
                status = PreviewFixtures.status.copy(
                    id = "101",
                    poll = PreviewFixtures.poll,
                    content = "<p>Which accent wins?</p>",
                ),
            )
        }
    }
}

@Preview(name = "Status card — light", showBackground = true, heightDp = 260)
@Composable
private fun StatusCardLightPreview() {
    PreviewHarness(darkTheme = false) {
        Column(Modifier.padding(16.dp)) {
            StatusCard(status = PreviewFixtures.status)
        }
    }
}

@Preview(name = "Status card — boost / CW / quote", showBackground = true, heightDp = 640)
@Composable
private fun StatusCardVariantsPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            StatusCard(status = PreviewFixtures.boostStatus)
            StatusCard(status = PreviewFixtures.cwStatus)
            StatusCard(status = PreviewFixtures.quoteStatus)
        }
    }
}

@Preview(name = "Status card — media", showBackground = true, heightDp = 400)
@Composable
private fun StatusCardMediaPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            StatusCard(status = PreviewFixtures.mediaStatus)
        }
    }
}

@Preview(name = "Status body — CW revealed states", showBackground = true, heightDp = 200)
@Composable
private fun StatusBodyPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            var revealed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            StatusBody(
                status = PreviewFixtures.cwStatus,
                revealed = revealed,
                onToggleReveal = { revealed = !revealed }
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
            StatusBody(status = PreviewFixtures.status)
        }
    }
}

@Preview(name = "Quote card", showBackground = true, heightDp = 220)
@Composable
private fun QuoteCardPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            QuoteCard(status = PreviewFixtures.status)
        }
    }
}

@Preview(name = "Poll — open + results", showBackground = true, heightDp = 420)
@Composable
private fun PollViewPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            PollView(poll = PreviewFixtures.openPoll)
            PollView(poll = PreviewFixtures.poll)
        }
    }
}

@Preview(name = "Media grid 1–4", showBackground = true, heightDp = 760)
@Composable
private fun MediaGridPreview() {
    val a = { id: String -> PreviewFixtures.attachment(id) }
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            MediaGrid(attachments = listOf(PreviewFixtures.attachment("1", type = "video")))
            MediaGrid(attachments = listOf(a("1"), a("2")))
            MediaGrid(attachments = listOf(a("1"), a("2"), a("3")))
            MediaGrid(attachments = listOf(a("1"), a("2"), a("3"), a("4")))
        }
    }
}

@Preview(name = "Status actions", showBackground = true, heightDp = 160)
@Composable
private fun StatusActionsPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            StatusActions(status = PreviewFixtures.status)
            StatusActions(status = PreviewFixtures.mediaStatus)
        }
    }
}

@Preview(name = "Account row", showBackground = true, heightDp = 220)
@Composable
private fun AccountRowPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            AccountRow(account = PreviewFixtures.account)
            AccountRow(account = PreviewFixtures.account2)
        }
    }
}

@Preview(name = "Status list skeleton", showBackground = true, heightDp = 480)
@Composable
private fun StatusListSkeletonPreview() {
    PreviewHarness {
        StatusListSkeleton(cards = 4)
    }
}

@Preview(name = "Error pane", showBackground = true, heightDp = 300)
@Composable
private fun ErrorPanePreview() {
    PreviewHarness {
        ErrorPane(message = "Could not reach mastodon.social", onRetry = {})
    }
}

@Preview(name = "Async list — empty", showBackground = true, heightDp = 300)
@Composable
private fun AsyncListEmptyPreview() {
    PreviewHarness {
        AsyncList(
            state = AsyncState.ready(emptyList<Status>()),
            onRefresh = {},
            emptyLabel = "No toots yet — follow some people!",
        ) { }
    }
}

@Preview(name = "Media preview screen", showBackground = true, heightDp = 480)
@Composable
private fun MediaPreviewScreenPreview() {
    PreviewHarness {
        MediaPreviewScreen(url = "")
    }
}
