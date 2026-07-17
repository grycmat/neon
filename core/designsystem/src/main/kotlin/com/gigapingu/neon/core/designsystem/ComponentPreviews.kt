package com.gigapingu.neon.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.designsystem.component.GlassButton
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.GlassField
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.GradientButton
import com.gigapingu.neon.core.designsystem.component.HtmlText
import com.gigapingu.neon.core.designsystem.component.NeonAvatar
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.component.NeonLabel
import com.gigapingu.neon.core.designsystem.component.SkeletonBlock
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.model.Account

private val previewAccount = Account(
    id = "1",
    username = "aurora",
    acct = "aurora@mastodon.social",
    displayName = "Aurora Vex",
)

@Preview(name = "Neon components — dark", showBackground = true, heightDp = 640)
@Composable
private fun NeonComponentsDarkPreview() {
    NeonTheme(darkTheme = true) {
        NeonBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                NeonLabel("Neon · Fediverse")
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Glass card", style = NeonTheme.type.titleMedium, color = NeonTheme.palette.text)
                }
                GradientButton(label = "Connect", trailingArrow = true, onClick = {}, modifier = Modifier.fillMaxWidth())
                GlassButton(label = "Log out", onClick = {}, modifier = Modifier.fillMaxWidth())
                GlassIconButton(icon = Icons.Rounded.Search, onClick = {})
                GlassField(label = "Instance") {
                    Text("mastodon.social", style = NeonTheme.type.bodyLarge, color = NeonTheme.palette.text)
                }
            }
        }
    }
}

@Preview(name = "Buttons — busy / disabled / tinted", showBackground = true, heightDp = 320)
@Composable
private fun ButtonStatesPreview() {
    NeonTheme(darkTheme = true) {
        NeonBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GradientButton(label = "Posting…", busy = true, onClick = {}, modifier = Modifier.fillMaxWidth())
                GradientButton(label = "Disabled", onClick = null, modifier = Modifier.fillMaxWidth())
                GlassButton(label = "Tinted", tinted = true, onClick = {}, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassIconButton(icon = Icons.Rounded.Search, onClick = {})
                    GlassIconButton(icon = Icons.Rounded.Search, tinted = true, onClick = {})
                }
            }
        }
    }
}

@Preview(name = "Avatar — sizes + ring", showBackground = true, heightDp = 160)
@Composable
private fun NeonAvatarPreview() {
    NeonTheme(darkTheme = true) {
        NeonBackground(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(22.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeonAvatar(account = previewAccount, size = 22.dp)
                NeonAvatar(account = previewAccount, size = 38.dp)
                NeonAvatar(account = previewAccount, size = 46.dp, ring = true)
                NeonAvatar(account = previewAccount, size = 72.dp, ring = true)
                NeonAvatar(account = null, size = 38.dp)
            }
        }
    }
}

@Preview(name = "Skeleton blocks", showBackground = true, heightDp = 180)
@Composable
private fun SkeletonPreview() {
    NeonTheme(darkTheme = true) {
        NeonBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SkeletonBlock(Modifier.size(38.dp), shape = CircleShape)
                SkeletonBlock(Modifier.height(12.dp).fillMaxWidth(.5f))
                SkeletonBlock(Modifier.height(10.dp).fillMaxWidth())
                SkeletonBlock(Modifier.height(10.dp).fillMaxWidth(.8f))
            }
        }
    }
}

@Preview(name = "Html text — mention / hashtag / link", showBackground = true, heightDp = 200)
@Composable
private fun HtmlTextPreview() {
    NeonTheme(darkTheme = true) {
        NeonBackground(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(22.dp)) {
                HtmlText(
                    html = "<p>Hey <a class=\"mention\" href=\"#\">@moss</a>, the " +
                        "<a class=\"hashtag\" href=\"#\">#neon</a> tulips are blooming — " +
                        "wrote it up at <a href=\"https://example.com\">example.com/garden</a>.</p>" +
                        "<p>Second paragraph, plain text.</p>",
                    onMentionClick = {},
                    onHashtagClick = {},
                    onLinkClick = {},
                )
            }
        }
    }
}

@Preview(name = "Neon components — light", showBackground = true, heightDp = 400)
@Composable
private fun NeonComponentsLightPreview() {
    NeonTheme(darkTheme = false) {
        NeonBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                NeonLabel("Neon · Fediverse")
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Glass card", style = NeonTheme.type.titleMedium, color = NeonTheme.palette.text)
                }
                GradientButton(label = "Connect", trailingArrow = true, onClick = {}, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
