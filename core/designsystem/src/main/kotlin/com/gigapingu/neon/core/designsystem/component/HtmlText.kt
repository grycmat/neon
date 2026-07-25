package com.gigapingu.neon.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.HtmlSegmentType
import com.gigapingu.neon.core.designsystem.util.parseStatusHtml
import com.gigapingu.neon.core.model.Emoji
import androidx.compose.runtime.getValue

/**
 * Renders Mastodon HTML content as rich text with tappable mentions,
 * hashtags and links, plus inline `:shortcode:` custom emoji.
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = NeonTheme.type.bodyMedium,
    maxLines: Int = Int.MAX_VALUE,
    emojis: List<Emoji> = emptyList(),
    onMentionClick: ((acctOrUrl: String) -> Unit)? = null,
    onHashtagClick: ((tag: String) -> Unit)? = null,
    onLinkClick: ((url: String) -> Unit)? = null,
) {
    val palette = NeonTheme.palette
    // Callbacks are typically fresh lambda instances every recomposition (they
    // close over caller state like `status`), which would otherwise defeat this
    // remember on every caller recomposition. rememberUpdatedState lets the
    // built spans always invoke the latest callback while keying the expensive
    // parse/AnnotatedString build only on whether a handler is present at all.
    val currentOnMentionClick by rememberUpdatedState(onMentionClick)
    val currentOnHashtagClick by rememberUpdatedState(onHashtagClick)
    val currentOnLinkClick by rememberUpdatedState(onLinkClick)
    val shortcodes = remember(emojis) { emojis.mapTo(HashSet()) { it.shortcode } }
    val text: AnnotatedString = remember(
        html,
        palette,
        shortcodes,
        onMentionClick != null,
        onHashtagClick != null,
        onLinkClick != null,
    ) {
        val segments = parseStatusHtml(html)
        buildAnnotatedString {
            val accentStyle = SpanStyle(color = palette.cyan, fontWeight = FontWeight.Bold)
            val linkStyle = SpanStyle(
                color = palette.purple,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
            )
            for (segment in segments) {
                when (segment.type) {
                    HtmlSegmentType.Text -> appendWithEmoji(segment.text, shortcodes)

                    HtmlSegmentType.Mention ->
                        clickableSpan(segment.text, accentStyle, onMentionClick?.let {
                            { currentOnMentionClick?.invoke(segment.href ?: segment.text) }
                        })

                    HtmlSegmentType.Hashtag ->
                        clickableSpan(segment.text, accentStyle, onHashtagClick?.let {
                            { currentOnHashtagClick?.invoke(segment.text.removePrefix("#")) }
                        })

                    HtmlSegmentType.Link ->
                        clickableSpan(segment.text, linkStyle, segment.href?.let { href ->
                            onLinkClick?.let {
                                { currentOnLinkClick?.invoke(href) }
                            }
                        })
                }
            }
        }
    }
    val inlineContent = rememberEmojiInlineContent(emojis)
    Text(
        text = text,
        style = style,
        color = style.color.takeUnless { it.isUnspecified } ?: palette.text,
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
        inlineContent = inlineContent,
        modifier = modifier,
    )
}

/**
 * Plain-text counterpart to [HtmlText] for fields that aren't HTML (display
 * names) but can still carry `:shortcode:` custom emoji.
 */
@Composable
fun EmojiText(
    text: String,
    emojis: List<Emoji> = emptyList(),
    modifier: Modifier = Modifier,
    style: TextStyle = NeonTheme.type.bodyMedium,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val shortcodes = remember(emojis) { emojis.mapTo(HashSet()) { it.shortcode } }
    val annotated = remember(text, shortcodes) {
        buildAnnotatedString { appendWithEmoji(text, shortcodes) }
    }
    val inlineContent = rememberEmojiInlineContent(emojis)
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = inlineContent,
    )
}

private val shortcodePattern = Regex(":([a-zA-Z0-9_+-]+):")

/** Splits [text] on `:shortcode:` runs that match a known emoji, inlining the rest verbatim. */
private fun AnnotatedString.Builder.appendWithEmoji(text: String, emojiShortcodes: Set<String>) {
    if (emojiShortcodes.isEmpty()) {
        append(text)
        return
    }
    var index = 0
    for (match in shortcodePattern.findAll(text)) {
        val code = match.groupValues[1]
        if (code !in emojiShortcodes) continue
        if (match.range.first > index) append(text.substring(index, match.range.first))
        appendInlineContent(code, ":$code:")
        index = match.range.last + 1
    }
    if (index < text.length) append(text.substring(index))
}

@Composable
private fun rememberEmojiInlineContent(emojis: List<Emoji>): Map<String, InlineTextContent> =
    remember(emojis) {
        emojis.associate { emoji ->
            emoji.shortcode to InlineTextContent(
                placeholder = Placeholder(1.15.em, 1.15.em, PlaceholderVerticalAlign.TextCenter),
            ) {
                AsyncImage(
                    model = emoji.url.ifEmpty { emoji.staticUrl },
                    contentDescription = ":${emoji.shortcode}:",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

private fun androidx.compose.ui.text.AnnotatedString.Builder.clickableSpan(
    text: String,
    style: SpanStyle,
    onClick: (() -> Unit)?,
) {
    if (onClick == null) {
        withStyle(style) { append(text) }
    } else {
        withLink(
            LinkAnnotation.Clickable(
                tag = text,
                styles = TextLinkStyles(style = style),
                linkInteractionListener = { onClick() },
            ),
        ) {
            append(text)
        }
    }
}
