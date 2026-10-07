package com.samupdater.app.ui.watchlist

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samupdater.app.ui.Loadable
import com.samupdater.app.ui.appViewModel
import com.samupdater.app.ui.components.DevicePickerDialog
import com.samupdater.app.ui.components.EmptyText
import com.samupdater.app.ui.theme.BuildNumberFont
import com.samupdater.app.ui.components.OneUiCard
import com.samupdater.app.ui.components.OneUiPage
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column

@Composable
fun WatchlistScreen(
    vm: WatchlistViewModel = viewModel(
        factory = appViewModel { WatchlistViewModel(it.repository, it.settings, it.deviceInfo.detectedModel()) },
    ),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }

    OneUiPage(
        title = "Watchlist",
        subtitle = "Every Galaxy you follow.",
        actions = { IconButton(onClick = vm::refresh) { Icon(Icons.Outlined.Refresh, "Refresh") } },
        floatingAction = if (state.isFull) null else {
            {
                ExtendedFloatingActionButton(
                    onClick = { adding = true },
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text("Add device") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = CircleShape,
                )
            }
        },
        bottomSpace = FAB_CLEARANCE,
    ) {
        if (state.entries.size <= 1) {
            item { EmptyText("Add a family member's phone, your tablet or any Galaxy model to follow its updates.") }
        }
        if (state.entries.isNotEmpty()) {
            item {
                OneUiCard {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        state.entries.forEachIndexed { index, entry ->
                            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 20.dp))
                            WatchRow(entry, onRemove = { vm.remove(entry.device) })
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        DevicePickerDialog(
            title = "Add to watchlist",
            initial = null,
            cscOptions = state.cscOptions,
            showNickname = true,
            onDismiss = { adding = false },
            onConfirm = { device ->
                vm.add(device)
                adding = false
            },
        )
    }
}

@Composable
private fun WatchRow(entry: WatchEntry, onRemove: () -> Unit) {
    val supporting = when (val firmware = entry.firmware) {
        Loadable.Loading -> "Checking"
        is Loadable.Failed -> firmware.message
        is Loadable.Ready -> listOfNotNull(firmware.value.latest.build, firmware.value.latest.releaseDate).joinToString("\n")
    }
    val headline = when {
        entry.isThisPhone -> "${entry.device.label} (this phone)"
        entry.isMine -> "${entry.device.label} (my device)"
        else -> entry.device.label
    }
    ListItem(
        leadingContent = { Icon(Icons.Outlined.Smartphone, null) },
        headlineContent = { Text(headline) },
        supportingContent = { Text(supporting, fontFamily = if (entry.firmware is Loadable.Ready) BuildNumberFont else null) },
        trailingContent = {
            if (!entry.isMine) IconButton(onClick = onRemove) { Icon(Icons.Outlined.Delete, "Remove ${entry.device.label}") }
        },
    )
}

private const val FAB_CLEARANCE = 96
