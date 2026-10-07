package com.samupdater.app.data.notes

import com.samupdater.app.data.site.HtmlText
import com.samupdater.app.domain.NotesBlock

/**
 * Reads one build's notes from a doc.samsungmobile.com page. The page lists every build for the model,
 * each with a grey box where headings are <b>, feature titles are <u> and lines end with <br>.
 */
object ReleaseNotesParser {

    private const val BUILD_MARKER = "Build Number : </strong>"
    private const val NOTES_BOX_MARKER = "background-color:#eee"
    private const val SPAN_OPEN = "<span"
    private const val SPAN_CLOSE = "</span>"
    private val LINE_BREAK = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val BOLD_LINE = Regex("^<b>(.+?)</b>$", RegexOption.IGNORE_CASE)
    private val UNDERLINE_LINE = Regex("^<u>(.+?)</u>$", RegexOption.IGNORE_CASE)

    /** Returns the blocks for [build], or an empty list if the page doesn't have that build. */
    fun parse(html: String, build: String): List<NotesBlock> {
        val body = notesHtml(html, build) ?: return emptyList()
        return body.split(LINE_BREAK).mapNotNull(::toBlock)
    }

    private fun notesHtml(html: String, build: String): String? {
        val buildAt = html.indexOf("$BUILD_MARKER$build", ignoreCase = true).takeIf { it >= 0 } ?: return null
        val boxAt = html.indexOf(NOTES_BOX_MARKER, buildAt).takeIf { it >= 0 } ?: return null
        val nextBuildAt = html.indexOf(BUILD_MARKER, buildAt + BUILD_MARKER.length).let { if (it < 0) html.length else it }
        if (boxAt > nextBuildAt) return null
        val spanAt = html.indexOf(SPAN_OPEN, boxAt).takeIf { it in 0 until nextBuildAt } ?: return null
        val start = html.indexOf('>', spanAt).takeIf { it >= 0 }?.plus(1) ?: return null
        val end = html.indexOf(SPAN_CLOSE, start).takeIf { it >= 0 } ?: return null
        return html.substring(start, end)
    }

    private fun toBlock(line: String): NotesBlock? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        BOLD_LINE.matchEntire(trimmed)?.let { return clean(it.groupValues[1])?.let(NotesBlock::Section) }
        UNDERLINE_LINE.matchEntire(trimmed)?.let { return clean(it.groupValues[1])?.let(NotesBlock::Feature) }
        return clean(trimmed)?.let(NotesBlock::Paragraph)
    }

    private fun clean(html: String): String? = HtmlText.clean(html).takeIf { it.isNotBlank() }
}
