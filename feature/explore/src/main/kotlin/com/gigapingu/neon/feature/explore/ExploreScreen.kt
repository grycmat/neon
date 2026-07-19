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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gigapingu.neon.core.designsystem.component.GlassCard
import com.gigapingu.neon.core.designsystem.component.GlassIconButton
import com.gigapingu.neon.core.designsystem.component.NeonBackground
import com.gigapingu.neon.core.designsystem.component.NeonLabel
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.compactCount
import com.gigapingu.neon.core.model.SearchResults
import com.gigapingu.neon.core.model.TrendTag
import com.gigapingu.neon.core.ui.AccountRow
import com.gigapingu.neon.core.ui.Navigator
import com.gigapingu.neon.core.ui.LocalShellPadding
import com.gigapingu.neon.core.ui.PreviewFixtures
import com.gigapingu.neon.core.ui.PreviewHarness
import com.gigapingu.neon.core.ui.hingePaneWidth
import com.gigapingu.neon.core.ui.isBigScreen
import com.gigapingu.neon.core.ui.status.StatusCard
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Explore: trending tags + toots, and full search (accounts / toots / tags).
 * Used both as the tab root and pushed with an [initialQuery] (hashtag taps).
 */
@Composable
fun ExploreScreen(
    initialQuery: String? = null,
    viewModel: ExploreViewModel = hiltViewModel(key = "explore-${initialQuery.orEmpty()}"),
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPushed = initialQuery != null
    val shellPadding = LocalShellPadding.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.start(initialQuery) }
    LaunchedEffect(Unit) {
        viewModel.errors.collect { snackbarHostState.showSnackbar(it) }
    }

    // The header (back row + search field) floats over the list, like
    // HomeShell's bars, so items actually scroll up behind the translucent
    // top app bar instead of stopping below it.
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerHeight = with(LocalDensity.current) { headerHeightPx.toDp() }

    // Big screens split into hinge-aligned panes: trends / people+tags left,
    // toots right. Null pane width = phone single column.
    val paneWidth = if (isBigScreen()) hingePaneWidth(inShell = !isPushed) else null

    NeonBackground {
        Box(Modifier.fillMaxSize()) {
            when {
                uiState.searching -> Box(
                    Modifier.fillMaxSize().padding(top = headerHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = palette.cyan)
                }
                uiState.results != null -> if (paneWidth != null) {
                    SearchResultsBig(
                        uiState,
                        onTagClick = viewModel::searchTag,
                        topPadding = headerHeight,
                        paneWidth = paneWidth,
                    )
                } else {
                    SearchResultsList(uiState, onTagClick = viewModel::searchTag, topPadding = headerHeight)
                }
                else -> if (paneWidth != null) {
                    TrendsBig(
                        uiState,
                        onTagClick = viewModel::searchTag,
                        topPadding = headerHeight,
                        paneWidth = paneWidth,
                    )
                } else {
                    Trends(uiState, onTagClick = viewModel::searchTag, topPadding = headerHeight)
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(palette.surfaceSolid.copy(alpha = 0.80f))
                    .onSizeChanged { headerHeightPx = it.height }
                    .run {
                        if (isPushed) statusBarsPadding() else padding(top = shellPadding.calculateTopPadding())
                    },
            ) {
                if (isPushed) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlassIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                            onClick = Navigator::back,
                            contentDescription = "Back",
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Explore", style = type.headlineMedium, color = palette.text)
                    }
                }
                SearchField(
                    query = uiState.query,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = viewModel::search,
                    onClear = viewModel::clearSearch,
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
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
private fun Trends(uiState: ExploreUiState, onTagClick: (String) -> Unit, topPadding: Dp = 0.dp) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    if (uiState.loadingTrends) {
        Box(Modifier.fillMaxSize().padding(top = topPadding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = palette.cyan)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            top = topPadding + 10.dp,
            end = 16.dp,
            bottom = 90.dp + LocalShellPadding.current.calculateBottomPadding(),
        ),
    ) {
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
private fun SearchResultsList(uiState: ExploreUiState, onTagClick: (String) -> Unit, topPadding: Dp = 0.dp) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    val results = uiState.results ?: return
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            top = topPadding + 10.dp,
            end = 16.dp,
            bottom = 90.dp + LocalShellPadding.current.calculateBottomPadding(),
        ),
    ) {
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

/** Big-screen trends: ranked tag cards left of the hinge, trending toots right. */
@Composable
private fun TrendsBig(
    uiState: ExploreUiState,
    onTagClick: (String) -> Unit,
    topPadding: Dp,
    paneWidth: Dp,
) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    if (uiState.loadingTrends) {
        Box(Modifier.fillMaxSize().padding(top = topPadding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = palette.cyan)
        }
        return
    }
    val contentPadding = PaddingValues(
        start = 16.dp,
        top = topPadding + 10.dp,
        end = 16.dp,
        bottom = 90.dp + LocalShellPadding.current.calculateBottomPadding(),
    )
    Row(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.width(paneWidth).fillMaxHeight(),
            contentPadding = contentPadding,
        ) {
            item {
                NeonLabel("Trending now", modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp))
            }
            if (uiState.tags.isEmpty()) {
                item { PaneEmpty("No trends available on this instance") }
            }
            items(count = uiState.tags.size, key = { uiState.tags[it].name }) { index ->
                val tag = uiState.tags[index]
                TrendCard(rank = index + 1, tag = tag, onClick = { onTagClick(tag.name) })
            }
        }
        PaneDivider()
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = contentPadding,
        ) {
            item {
                NeonLabel("Trending toots", modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp))
            }
            if (uiState.trending.isEmpty()) {
                item { PaneEmpty("No trends available on this instance") }
            }
            items(count = uiState.trending.size, key = { uiState.trending[it].id }) { index ->
                StatusCard(status = uiState.trending[index])
            }
        }
    }
}

/** Big-screen search results: people + tags left of the hinge, toots right. */
@Composable
private fun SearchResultsBig(
    uiState: ExploreUiState,
    onTagClick: (String) -> Unit,
    topPadding: Dp,
    paneWidth: Dp,
) {
    val results = uiState.results ?: return
    val contentPadding = PaddingValues(
        start = 16.dp,
        top = topPadding + 10.dp,
        end = 16.dp,
        bottom = 90.dp + LocalShellPadding.current.calculateBottomPadding(),
    )
    Row(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.width(paneWidth).fillMaxHeight(),
            contentPadding = contentPadding,
        ) {
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
                }
            }
            if (results.accounts.isEmpty() && results.hashtags.isEmpty()) {
                item { PaneEmpty("No people or tags found") }
            }
        }
        PaneDivider()
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = contentPadding,
        ) {
            item {
                NeonLabel("Toots", modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp))
            }
            if (results.statuses.isEmpty()) {
                item { PaneEmpty("No toots found") }
            }
            items(count = results.statuses.size, key = { "s" + results.statuses[it].id }) { index ->
                StatusCard(status = results.statuses[index])
            }
        }
    }
}

@Composable
private fun PaneDivider() {
    Box(
        Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(NeonTheme.palette.divider),
    )
}

@Composable
private fun PaneEmpty(message: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 50.dp), contentAlignment = Alignment.Center) {
        Text(message, style = NeonTheme.type.bodyMedium, color = NeonTheme.palette.textMute)
    }
}

/** Ranked trend row (design 04): rank · tag + uses · usage spark bars. */
@Composable
private fun TrendCard(rank: Int, tag: TrendTag, onClick: () -> Unit) {
    val palette = NeonTheme.palette
    val type = NeonTheme.type
    GlassCard(
        modifier = Modifier.padding(vertical = 5.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$rank",
                style = type.titleMedium,
                color = palette.textMute,
                modifier = Modifier.width(26.dp),
            )
            Column(Modifier.weight(1f)) {
                Text("#${tag.name}", style = type.titleSmall, color = palette.text)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${compactCount(tag.uses)} toots",
                    style = type.bodySmall,
                    color = palette.textDim,
                )
            }
            TrendSpark(tag)
        }
    }
}

/** Tiny bar chart of the tag's daily uses, oldest → newest. */
@Composable
private fun TrendSpark(tag: TrendTag) {
    val values = tag.history.mapNotNull { day ->
        ((day as? JsonObject)?.get("uses") as? JsonPrimitive)?.content?.toFloatOrNull()
    }.reversed().takeLast(8)
    if (values.size < 2) return
    val palette = NeonTheme.palette
    val max = values.max().coerceAtLeast(1f)
    Row(
        modifier = Modifier.height(24.dp).padding(start = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEach { value ->
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight((value / max).coerceIn(.18f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(listOf(palette.cyan, palette.purple))),
            )
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

private fun previewTag(name: String, uses: Int) = TrendTag(
    name = name,
    history = listOf(buildJsonObject { put("uses", uses) }),
)

private val previewTags = listOf(
    previewTag("neon", 4210),
    previewTag("synthwave", 1930),
    previewTag("fediverse", 812),
    previewTag("gardening", 245),
)

@Preview(name = "Search field", showBackground = true, heightDp = 180)
@Composable
private fun SearchFieldPreview() {
    PreviewHarness {
        Column {
            SearchField(query = "", onQueryChange = {}, onSearch = {}, onClear = {})
            SearchField(query = "neon tulips", onQueryChange = {}, onSearch = {}, onClear = {})
        }
    }
}

@Preview(name = "Explore trends", showBackground = true, heightDp = 520)
@Composable
private fun TrendsPreview() {
    PreviewHarness {
        Trends(
            uiState = ExploreUiState(
                loadingTrends = false,
                tags = previewTags,
                trending = listOf(PreviewFixtures.status),
            ),
            onTagClick = {},
        )
    }
}

@Preview(name = "Search results", showBackground = true, heightDp = 620)
@Composable
private fun SearchResultsListPreview() {
    PreviewHarness {
        SearchResultsList(
            uiState = ExploreUiState(
                query = "neon",
                results = SearchResults(
                    accounts = listOf(PreviewFixtures.account, PreviewFixtures.account2),
                    statuses = listOf(PreviewFixtures.status),
                    hashtags = previewTags.take(2),
                ),
            ),
            onTagClick = {},
        )
    }
}
