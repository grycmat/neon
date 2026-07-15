package com.gigapingu.neon.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.component.NeonLabel
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.compactCount
import com.gigapingu.neon.core.model.TrendTag
import com.gigapingu.neon.core.ui.AccountRow
import com.gigapingu.neon.core.ui.LocalNeonNavigator
import com.gigapingu.neon.core.ui.status.StatusCard

/**
 * Explore: trending tags + toots, and full search (accounts / toots / tags).
 * Used both as the tab root and pushed with an [initialQuery] (hashtag taps).
 */
@Composable
fun ExploreScreen(
    initialQuery: String? = null,
    snackbarHostState: SnackbarHostState? = null,
    viewModel: ExploreViewModel = hiltViewModel(key = "explore-${initialQuery.orEmpty()}"),
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val navigator = LocalNeonNavigator.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPushed = initialQuery != null

    LaunchedEffect(Unit) { viewModel.start(initialQuery) }
    if (snackbarHostState != null) {
        LaunchedEffect(Unit) {
            viewModel.errors.collect { snackbarHostState.showSnackbar(it) }
        }
    }

    NeonBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isPushed) {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                        onClick = navigator::back,
                        contentDescription = "Back",
                    )
                    Spacer(Modifier.width(10.dp))
                } else {
                    Spacer(Modifier.width(8.dp))
                }
                Text("Explore", style = type.headlineMedium, color = palette.text)
            }
            SearchField(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = viewModel::search,
                onClear = viewModel::clearSearch,
            )
            when {
                uiState.searching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.cyan)
                }
                uiState.results != null -> SearchResultsList(uiState, onTagClick = viewModel::searchTag)
                else -> Trends(uiState, onTagClick = viewModel::searchTag)
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
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
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = type.bodyLarge.copy(color = palette.text),
            cursorBrush = SolidColor(palette.cyan),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 14.dp),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text("Search the fediverse…", style = type.bodyLarge, color = palette.textMute)
                }
                innerTextField()
            },
        )
        if (query.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Clear search",
                tint = palette.textMute,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onClear),
            )
        }
    }
}

@Composable
private fun Trends(uiState: ExploreUiState, onTagClick: (String) -> Unit) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    if (uiState.loadingTrends) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = palette.cyan)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 90.dp)) {
        if (uiState.tags.isNotEmpty()) {
            item {
                NeonLabel("Trending tags", modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 10.dp))
                TagWrap(tags = uiState.tags, showUses = true, onTagClick = onTagClick)
                Spacer(Modifier.height(20.dp))
            }
        }
        if (uiState.trending.isNotEmpty()) {
            item {
                NeonLabel("Trending toots", modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp))
            }
            items(count = uiState.trending.size, key = { uiState.trending[it].id }) { index ->
                StatusCard(status = uiState.trending[index])
            }
        }
        if (uiState.tags.isEmpty() && uiState.trending.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No trends available on this instance",
                        style = type.bodyMedium,
                        color = palette.textMute,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsList(uiState: ExploreUiState, onTagClick: (String) -> Unit) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val results = uiState.results ?: return
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 90.dp)) {
        if (results.accounts.isNotEmpty()) {
            item {
                NeonLabel("People", modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp))
            }
            items(count = results.accounts.size, key = { "a" + results.accounts[it].id }) { index ->
                AccountRow(account = results.accounts[index])
            }
            item { Spacer(Modifier.height(14.dp)) }
        }
        if (results.hashtags.isNotEmpty()) {
            item {
                NeonLabel("Tags", modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 10.dp))
                TagWrap(tags = results.hashtags, showUses = false, onTagClick = onTagClick)
                Spacer(Modifier.height(14.dp))
            }
        }
        if (results.statuses.isNotEmpty()) {
            item {
                NeonLabel("Toots", modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp))
            }
            items(count = results.statuses.size, key = { "s" + results.statuses[it].id }) { index ->
                StatusCard(status = results.statuses[index])
            }
        }
        if (results.accounts.isEmpty() && results.statuses.isEmpty() && results.hashtags.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) {
                    Text("Nothing found", style = type.bodyMedium, color = palette.textMute)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagWrap(tags: List<TrendTag>, showUses: Boolean, onTagClick: (String) -> Unit) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        tags.forEach { tag ->
            val shape = RoundedCornerShape(13.dp)
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(palette.gradientSoft)
                    .border(1.dp, palette.borderStrong, shape)
                    .clickable { onTagClick(tag.name) }
                    .padding(horizontal = 13.dp, vertical = 9.dp),
            ) {
                Text(
                    if (showUses) "#${tag.name} · ${compactCount(tag.uses)}" else "#${tag.name}",
                    style = type.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.text,
                )
            }
        }
    }
}
