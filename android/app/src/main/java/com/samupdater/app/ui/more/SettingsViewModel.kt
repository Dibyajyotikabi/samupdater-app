package com.samupdater.app.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samupdater.app.data.prefs.AlertSettings
import com.samupdater.app.data.prefs.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settings: SettingsStore) : ViewModel() {

    val alerts: StateFlow<AlertSettings> = settings.settings
        .map { it.alerts }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AlertSettings())

    fun update(transform: (AlertSettings) -> AlertSettings) {
        viewModelScope.launch { settings.setAlerts(transform(alerts.value)) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
