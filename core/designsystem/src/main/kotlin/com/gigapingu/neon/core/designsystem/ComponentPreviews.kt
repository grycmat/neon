package com.gigapingu.neon.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.designsystem.component.GlassButton
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.GlassField
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.GradientButton
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.component.NeonLabel
import com.gigapingu.neon.core.designsystem.theme.NeonTheme

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
