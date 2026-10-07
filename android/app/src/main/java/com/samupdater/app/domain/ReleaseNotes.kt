package com.samupdater.app.domain

/** One piece of Samsung's release notes, kept in order so the screen can show the original layout. */
sealed interface NotesBlock {
    val text: String

    /** Bold group heading such as "Galaxy AI" or "Samsung Health". */
    data class Section(override val text: String) : NotesBlock

    /** Underlined feature title such as "Enhanced Now nudge". */
    data class Feature(override val text: String) : NotesBlock

    data class Paragraph(override val text: String) : NotesBlock
}
