package com.samupdater.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.samupdater.app.domain.CscOption
import com.samupdater.app.domain.TrackedDevice

object DeviceInput {
    private val MODEL = Regex("^SM-[A-Z0-9]{4,12}$")
    private val CSC = Regex("^[A-Z0-9]{3}$")

    fun normalizeModel(text: String): String {
        val upper = text.trim().uppercase()
        return if (upper.isNotEmpty() && !upper.startsWith("SM-")) "SM-$upper" else upper
    }

    fun isValidModel(model: String) = MODEL.matches(model)
    fun isValidCsc(csc: String) = CSC.matches(csc)
}

/** Model + CSC entry used for first setup, changing your device and adding to the watchlist. */
@Composable
fun DevicePickerDialog(
    title: String,
    initial: TrackedDevice?,
    cscOptions: List<CscOption>,
    showNickname: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (TrackedDevice) -> Unit,
) {
    var model by remember { mutableStateOf(initial?.model.orEmpty()) }
    var csc by remember { mutableStateOf(initial?.csc.orEmpty()) }
    var nickname by remember { mutableStateOf(initial?.nickname.orEmpty()) }
    val normalized = DeviceInput.normalizeModel(model)
    val modelOk = DeviceInput.isValidModel(normalized)
    val cscOk = DeviceInput.isValidCsc(csc)
    val matches = remember(csc, cscOptions) {
        if (csc.isBlank()) cscOptions else cscOptions.filter {
            it.code.startsWith(csc, true) || it.country.contains(csc, true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = model, onValueChange = { model = it.take(MAX_MODEL) },
                    label = { Text("Model number, like SM-S938B") },
                    isError = model.isNotBlank() && !modelOk, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                )
                OutlinedTextField(
                    value = csc, onValueChange = { csc = it.uppercase().take(CSC_LENGTH) },
                    label = { Text("CSC, like INS or EUX") },
                    isError = csc.length == CSC_LENGTH && !cscOk, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                )
                if (!cscOk && matches.isNotEmpty()) {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 180.dp)) {
                        items(matches, key = { it.code }) { option ->
                            ListItem(
                                headlineContent = { Text(option.code) },
                                supportingContent = { Text(option.country) },
                                modifier = Modifier.clickable { csc = option.code },
                            )
                        }
                    }
                }
                if (showNickname) {
                    OutlinedTextField(
                        value = nickname, onValueChange = { nickname = it.take(MAX_NICKNAME) },
                        label = { Text("Nickname (optional)") }, singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = modelOk && cscOk,
                onClick = { onConfirm(TrackedDevice(normalized, csc, nickname.trim().ifBlank { null })) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private const val MAX_MODEL = 16
private const val CSC_LENGTH = 3
private const val MAX_NICKNAME = 30
