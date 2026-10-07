package com.samupdater.app.ui.more

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samupdater.app.BuildConfig
import com.samupdater.app.ui.appViewModel
import com.samupdater.app.ui.components.EmptyText
import com.samupdater.app.ui.components.InfoRow
import com.samupdater.app.ui.components.SectionCard
import com.samupdater.app.work.FirmwareCheckWorker
import com.samupdater.app.ui.components.OneUiPage
import com.samupdater.app.ui.components.PillButton

@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel(factory = appViewModel { SettingsViewModel(it.settings) })) {
    val alerts by vm.alerts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var canNotify by remember { mutableStateOf(hasNotificationPermission(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { canNotify = it }

    OneUiPage(title = "Alerts and settings", subtitle = "Pick what's worth a ping.", onBack = onBack) {
        item {
            SectionCard("Notify me about") {
                if (!canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    EmptyText("Notifications are off for Sam Updater.")
                    FilledTonalButton(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text("Allow notifications")
                    }
                }
                AlertSwitch("New stable update", "When a new build rolls out for your devices", alerts.stable) { on ->
                    vm.update { it.copy(stable = on) }
                }
                AlertSwitch("New One UI beta", "When a newer beta build is listed", alerts.beta) { on ->
                    vm.update { it.copy(beta = on) }
                }
                AlertSwitch("Test builds spotted", "When Samsung starts testing new firmware", alerts.test) { on ->
                    vm.update { it.copy(test = on) }
                }
                PillButton(
                    text = "Check now",
                    icon = null,
                    onClick = {
                        FirmwareCheckWorker.runNow(context)
                        Toast.makeText(context, "Checking for updates", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
        item {
            SectionCard("About") {
                InfoRow("Version", BuildConfig.VERSION_NAME)
                InfoRow("Data", "samupdater.com and Samsung's update server")
                EmptyText("Sam Updater only shows firmware information. It never downloads or installs anything on your phone.")
            }
        }
    }
}

@Composable
private fun AlertSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}

private fun hasNotificationPermission(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
