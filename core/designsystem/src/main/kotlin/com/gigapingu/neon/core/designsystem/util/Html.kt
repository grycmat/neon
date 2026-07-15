package com.gigapingu.neon.core.designsystem.util

/**
 * Minimal HTML handling for Mastodon status/bio content, ported from the
 * Flutter utils/html.dart. Tokenizes into inline segments (text, mention,
 * hashtag, link) that HtmlText renders as tappable spans. Handles <p>, <br>,
 * <a> (classified by class attribute), ignores everything else.
 */
enum class HtmlSegmentType { Text, Mention, Hashtag, Link }

data class HtmlSegment(
    val type: HtmlSegmentType,
    val text: String,
    val href: String? = null,
)

private val entityMap = mapOf(
    "&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"",
    "&#39;" to "'", "&apos;" to "'", "&nbsp;" to " ",
)

private val numericEntity = Regex("&#(\\d+);")
private val brTag = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
private val pBreak = Regex("</p>\\s*<p[^>]*>", RegexOption.IGNORE_CASE)
private val pTag = Regex("</?p[^>]*>", RegexOption.IGNORE_CASE)
private val anyTag = Regex("<[^>]+>")
private val anchorTag = Regex("<a\\s+([^>]*)>(.*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val hrefAttr = Regex("href=\"([^\"]*)\"")
private val classAttr = Regex("class=\"([^\"]*)\"")

fun decodeEntities(s: String): String {
    var out = s
    entityMap.forEach { (k, v) -> out = out.replace(k, v) }
    return numericEntity.replace(out) { m ->
        m.groupValues[1].toIntOrNull()?.let { code -> String(Character.toChars(code)) } ?: m.value
    }
}

/** Strips all tags — for previews and plain-text uses. */
fun htmlToPlainText(html: String): String =
    parseStatusHtml(html).joinToString("") { it.text }

fun parseStatusHtml(html: String): List<HtmlSegment> {
    val segments = mutableListOf<HtmlSegment>()
    val text = html
        .replace(brTag, "\n")
        .replace(pBreak, "\n\n")
        .replace(pTag, "")

    var index = 0
    for (match in anchorTag.findAll(text)) {
        if (match.range.first > index) {
            addText(segments, text.substring(index, match.range.first))
        }
        val attrs = match.groupValues[1]
        val inner = decodeEntities(match.groupValues[2].replace(anyTag, ""))
        val href = hrefAttr.find(attrs)?.groupValues?.get(1)?.let(::decodeEntities)
        val classes = classAttr.find(attrs)?.groupValues?.get(1).orEmpty()
        val segment = when {
            "mention" in classes || inner.startsWith("@") ->
                HtmlSegment(HtmlSegmentType.Mention, inner, href)
            "hashtag" in classes || inner.startsWith("#") ->
                HtmlSegment(HtmlSegmentType.Hashtag, inner, href)
            else -> HtmlSegment(HtmlSegmentType.Link, inner, href)
        }
        segments += segment
        index = match.range.last + 1
    }
    if (index < text.length) addText(segments, text.substring(index))
    // Trim trailing whitespace-only segment.
    while (segments.isNotEmpty() &&
        segments.last().type == HtmlSegmentType.Text &&
        segments.last().text.isBlank()
    ) {
        segments.removeAt(segments.lastIndex)
    }
    return segments
}

private fun addText(segments: MutableList<HtmlSegment>, raw: String) {
    val cleaned = decodeEntities(raw.replace(anyTag, ""))
    if (cleaned.isEmpty()) return
    segments += HtmlSegment(HtmlSegmentType.Text, cleaned)
}
