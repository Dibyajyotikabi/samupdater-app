package com.samupdater.app.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samupdater.app.domain.NotesBlock

/** Samsung's notes with their headings and breaks. Long notes start collapsed. */
@Composable
internal fun ReleaseNotesView(blocks: List<NotesBlock>) {
    var expanded by rememberSaveable(blocks) { mutableStateOf(false) }
    val shown = if (expanded) blocks else blocks.take(COLLAPSED_BLOCKS)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        shown.forEach { NotesBlockText(it) }
        if (blocks.size > COLLAPSED_BLOCKS) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Show less" else "Show all notes")
            }
        }
    }
}

@Composable
private fun NotesBlockText(block: NotesBlock) {
    when (block) {
        is NotesBlock.Section -> Text(
            block.text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )
        is NotesBlock.Feature -> Text(
            block.text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )
        is NotesBlock.Paragraph -> Text(block.text, style = MaterialTheme.typography.bodySmall)
    }
}

private const val COLLAPSED_BLOCKS = 8
