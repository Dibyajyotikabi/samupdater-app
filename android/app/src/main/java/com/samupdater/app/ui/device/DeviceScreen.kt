package com.samupdater.app.ui.device

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samupdater.app.domain.TrackedDevice
import com.samupdater.app.ui.appViewModel
import com.samupdater.app.ui.components.DevicePickerDialog
import com.samupdater.app.ui.components.LoadableContent
import com.samupdater.app.ui.components.AppLogo
import com.samupdater.app.ui.components.OneUiPage

@Composable
fun DeviceScreen(
    vm: DeviceViewModel = viewModel(factory = appViewModel { DeviceViewModel(it.repository, it.settings, it.deviceInfo) }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }

    OneUiPage(
        title = "Sam Updater",
        subtitle = "Samsung software update checker",
        logo = { AppLogo() },
        actions = { IconButton(onClick = vm::refresh) { Icon(Icons.Outlined.Refresh, "Refresh") } },
    ) {
        if (state.needsSetup) {
            item { SetupPrompt(state.profile?.isSamsung == true) { editing = true } }
        } else {
            item { DeviceSelectorCard(state) { editing = true } }
            item { LoadableContent(state.firmware, onRetry = vm::refresh) { info -> HeroCard(state, info) } }
            item { InstalledCard(state) }
            item { OutlookCard(state) }
            item { LatestCard(state) }
        }
        item { SiteLinkCard() }
    }

    if (editing) {
        val profile = state.profile
        DevicePickerDialog(
            title = "Your device",
            initial = state.tracked ?: profile?.model?.let { TrackedDevice(it, profile.csc.orEmpty()) },
            cscOptions = state.catalog.cscSuggestions,
            showNickname = false,
            onDismiss = { editing = false },
            onConfirm = {
                vm.saveDevice(it)
                editing = false
            },
        )
    }
}
