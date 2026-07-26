package com.gigapingu.neon.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.ui.AccountRow
import com.gigapingu.neon.core.ui.Navigator

/** Recipient picker for starting a new direct message. */
@Composable
fun NewMessageScreen(viewModel: NewMessageViewModel = hiltViewModel()) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val results by viewModel.results.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                onClick = Navigator::back,
                contentDescription = "Back",
            )
            Spacer(Modifier.width(10.dp))
            Text("New message", style = type.headlineMedium, color = palette.text)
        }

        val shape = RoundedCornerShape(16.dp)
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 4.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(palette.surface)
                .border(1.dp, palette.border, shape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = palette.textMute,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.onQueryChange(it)
                },
                singleLine = true,
                textStyle = type.bodyLarge.copy(color = palette.text),
                cursorBrush = SolidColor(palette.cyan),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("Search for someone…", style = type.bodyLarge, color = palette.textMute)
                    }
                    inner()
                },
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(results, key = { it.id }) { account ->
                AccountRow(
                    account = account,
                    onClick = {
                        Navigator.back()
                        Navigator.openCompose(directToHandle = account.acct)
                    },
                )
            }
        }
    }
}
