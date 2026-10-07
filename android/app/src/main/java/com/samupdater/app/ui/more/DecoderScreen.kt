package com.samupdater.app.ui.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.samupdater.app.domain.BuildExplanation
import com.samupdater.app.domain.BuildPart
import com.samupdater.app.domain.FirmwareDecoder
import com.samupdater.app.ui.components.EmptyText
import com.samupdater.app.ui.components.SectionCard
import com.samupdater.app.ui.theme.BuildNumberFont
import com.samupdater.app.ui.components.OneUiPage

@Composable
fun DecoderScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    val build = remember(input) { FirmwareDecoder.decode(input) }

    OneUiPage(title = "Firmware decoder", subtitle = "Read any Samsung build number.", onBack = onBack) {
        item {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.uppercase().take(MAX_INPUT) },
                label = { Text("Build number, like S938BXXU4BYJ2") },
                singleLine = true,
                isError = input.isNotBlank() && build == null,
                supportingText = { if (input.isNotBlank() && build == null) Text("That doesn't look like a Samsung build number.") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = BuildNumberFont),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            if (build == null) {
                EmptyText("You'll find your build number in Settings > About phone > Software information > Build number.")
            } else {
                SectionCard(build.raw) { BuildExplanation.explain(build).forEach { PartRow(it) } }
            }
        }
    }
}

@Composable
private fun PartRow(part: BuildPart) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(part.chars, fontFamily = BuildNumberFont, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(88.dp))
        Column {
            Text(part.title, style = MaterialTheme.typography.titleSmall)
            Text(part.meaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val MAX_INPUT = 24
