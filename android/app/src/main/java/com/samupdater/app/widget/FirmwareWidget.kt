package com.samupdater.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.samupdater.app.MainActivity
import com.samupdater.app.SamUpdaterApp
import com.samupdater.app.data.getOrNull
import com.samupdater.app.domain.UpdateState
import com.samupdater.app.domain.UpdateStatus

private data class WidgetState(val title: String, val latest: String?, val status: String)

class FirmwareWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadState(context)
        provideContent { GlanceTheme { WidgetContent(state) } }
    }

    private suspend fun loadState(context: Context): WidgetState {
        val container = (context.applicationContext as SamUpdaterApp).container
        val device = container.settings.current().myDevice
            ?: return WidgetState("Sam Updater", null, "Open the app to pick your device")
        val latest = container.repository.firmware(device.model, device.csc).getOrNull()?.latest?.build
        val installed = container.deviceInfo.read().takeIf { it.model == device.model }?.pdaBuild
        val status = when (UpdateStatus.compare(installed, latest)) {
            UpdateState.UP_TO_DATE -> "You're up to date"
            UpdateState.UPDATE_AVAILABLE -> "Update available"
            UpdateState.ON_BETA -> "You're on a beta build"
            UpdateState.UNKNOWN -> if (latest == null) "Couldn't check right now" else "Latest for ${device.csc}"
        }
        return WidgetState(device.label, latest, status)
    }

    companion object {
        suspend fun refreshAll(context: Context) {
            try {
                FirmwareWidget().updateAll(context)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("SamUpdater", "Widget refresh failed", e)
            }
        }
    }
}

@Composable
private fun WidgetContent(state: WidgetState) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(state.title, style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 14.sp))
        Text(
            state.latest ?: "-",
            style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 18.sp, fontWeight = FontWeight.Bold),
        )
        Text(state.status, style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 12.sp))
    }
}

class FirmwareWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FirmwareWidget()
}
