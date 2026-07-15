package com.gigapingu.neon.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.model.Account
import com.gigapingu.neon.core.model.Poll
import com.gigapingu.neon.core.model.PollOption
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.ui.status.StatusCard

/** Sample data + no-op locals so status components can be previewed in isolation. */
object PreviewFixtures {
    val account = Account(
        id = "1",
        username = "aurora",
        acct = "aurora@mastodon.social",
        displayName = "Aurora Vex",
        note = "<p>Synthwave gardener. She/her.</p>",
    )

    val status = Status(
        id = "100",
        account = account,
        content = "<p>Just planted a row of <a class=\"hashtag\" href=\"#\">#neon</a> tulips — " +
            "they hum quietly at dusk. cc <a class=\"mention\" href=\"#\">@moss</a></p>",
        repliesCount = 3,
        reblogsCount = 12,
        favouritesCount = 48,
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

    val navigator = object : NeonNavigator {
        override fun openThread(statusId: String) = Unit
        override fun openProfile(accountId: String) = Unit
        override fun openHashtag(tag: String) = Unit
        override fun openCompose(replyToId: String?, quotingId: String?) = Unit
        override fun openFollowList(accountId: String, handle: String, following: Boolean) = Unit
        override fun openEditProfile() = Unit
        override fun openSettings() = Unit
        override fun back() = Unit
    }

    val actionHandler = object : StatusActionHandler {
        override fun toggleFavourite(status: Status) = Unit
        override fun toggleBoost(status: Status) = Unit
        override fun vote(poll: Poll, choices: List<Int>) = Unit
        override fun share(status: Status) = Unit
        override fun openMention(status: Status, acctOrUrl: String) = Unit
    }
}

@Composable
fun PreviewHarness(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    NeonTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(
            LocalNeonNavigator provides PreviewFixtures.navigator,
            LocalStatusActionHandler provides PreviewFixtures.actionHandler,
        ) {
            NeonBackground(modifier = Modifier.fillMaxSize()) {
                content()
            }
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

@Preview(name = "Account row", showBackground = true, heightDp = 140)
@Composable
private fun AccountRowPreview() {
    PreviewHarness {
        Column(Modifier.padding(16.dp)) {
            AccountRow(account = PreviewFixtures.account)
        }
    }
}
