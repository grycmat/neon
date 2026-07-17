package com.gigapingu.neon.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.NeonAvatar
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.htmlToPlainText
import com.gigapingu.neon.core.model.Account

/** One account in a list (followers, following, search results). */
@Composable
fun AccountRow(
    account: Account,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val bio = htmlToPlainText(account.note).replace('\n', ' ').trim()

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        onClick = { Navigator.openProfile(account.id) },
    ) {
        Row {
            NeonAvatar(account = account, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    account.displayNameOrUsername,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = type.titleSmall,
                    color = palette.text,
                )
                Text(
                    account.fullHandle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = type.bodySmall,
                    color = palette.textDim,
                )
                if (bio.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        bio,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = type.bodySmall,
                        color = palette.textDim,
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(10.dp))
                trailing()
            }
        }
    }
}
