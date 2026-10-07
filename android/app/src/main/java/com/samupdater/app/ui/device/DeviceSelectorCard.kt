package com.samupdater.app.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.samupdater.app.ui.Loadable
import com.samupdater.app.ui.components.CapsLabel
import com.samupdater.app.ui.components.OneUiCard

/** "YOUR DEVICE" card. Tapping it opens the model and CSC picker. */
@Composable
internal fun DeviceSelectorCard(state: DeviceUiState, onClick: () -> Unit) {
    val firmwareName = (state.firmware as? Loadable.Ready)?.value?.deviceName
    val name = state.catalogDevice?.name ?: firmwareName ?: state.tracked?.model ?: "Pick your device"
    val details = listOfNotNull(
        state.tracked?.model,
        state.tracked?.csc?.takeIf { it.isNotBlank() },
        if (state.isThisPhone) "This phone" else "Picked by you",
    ).joinToString(" · ")
    OneUiCard(Modifier.clip(MaterialTheme.shapes.large).clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(TILE_SIZE.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(TILE_CORNER.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.PhoneAndroid, null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                CapsLabel("Your device")
                Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 2.dp))
                Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ExpandMore, "Change device", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val TILE_SIZE = 52
private const val TILE_CORNER = 16
