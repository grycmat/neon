package com.gigapingu.neon.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
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
import com.gigapingu.neon.core.designsystem.theme.NeonTheme
import com.gigapingu.neon.core.designsystem.util.HtmlSegmentType
import com.gigapingu.neon.core.designsystem.util.parseStatusHtml

/**
 * Renders Mastodon HTML content as rich text with tappable mentions,
 * hashtags and links.
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = NeonTheme.type.bodyMedium,
    maxLines: Int = Int.MAX_VALUE,
    onMentionClick: ((acctOrUrl: String) -> Unit)? = null,
    onHashtagClick: ((tag: String) -> Unit)? = null,
    onLinkClick: ((url: String) -> Unit)? = null,
) {
    val palette = NeonTheme.palette
    val text: AnnotatedString = remember(html, palette, onMentionClick, onHashtagClick, onLinkClick) {
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
                    HtmlSegmentType.Text -> append(segment.text)

                    HtmlSegmentType.Mention ->
                        clickableSpan(segment.text, accentStyle, onMentionClick?.let { handler ->
                            { handler(segment.href ?: segment.text) }
                        })

                    HtmlSegmentType.Hashtag ->
                        clickableSpan(segment.text, accentStyle, onHashtagClick?.let { handler ->
                            { handler(segment.text.removePrefix("#")) }
                        })

                    HtmlSegmentType.Link ->
                        clickableSpan(segment.text, linkStyle, segment.href?.let { href ->
                            onLinkClick?.let { handler -> { handler(href) } }
                        })
                }
            }
        }
    }
    Text(
        text = text,
        style = style,
        color = style.color.takeUnless { it.isUnspecified } ?: palette.text,
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
        modifier = modifier,
    )
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
