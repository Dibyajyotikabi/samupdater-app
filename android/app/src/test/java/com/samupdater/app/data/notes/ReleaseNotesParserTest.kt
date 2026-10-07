package com.samupdater.app.data.notes

import com.samupdater.app.domain.NotesBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesParserTest {

    private val html = requireNotNull(javaClass.getResource("/notes/S938B_notes.html")) { "Missing fixture" }.readText()

    @Test
    fun `keeps headings, feature titles and paragraphs apart`() {
        val blocks = ReleaseNotesParser.parse(html, "S938BXXUCDZIF")

        assertEquals(NotesBlock.Section("One UI 9"), blocks[0])
        assertEquals(NotesBlock.Section("Galaxy AI"), blocks[1])
        assertEquals(NotesBlock.Feature("Enhanced Now nudge"), blocks[2])
        assertTrue(blocks[3] is NotesBlock.Paragraph)
        assertTrue(blocks[3].text.startsWith("Now nudge provides helpful suggestions"))
    }

    @Test
    fun `drops the empty lines between sections`() {
        val blocks = ReleaseNotesParser.parse(html, "S938BXXUCDZIF")

        assertTrue(blocks.none { it.text.isBlank() })
    }

    @Test
    fun `reads only the requested build`() {
        val previous = ReleaseNotesParser.parse(html, "S938BXXSCCZH1")

        assertEquals(3, previous.size)
        assertEquals("The device is protected with improved security.", previous[1].text)
    }

    @Test
    fun `returns nothing for a build the page doesn't list`() {
        assertTrue(ReleaseNotesParser.parse(html, "S938BXXU1AYA1").isEmpty())
    }
}
