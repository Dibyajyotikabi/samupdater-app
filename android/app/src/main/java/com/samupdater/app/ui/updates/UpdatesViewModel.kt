package com.samupdater.app.ui.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samupdater.app.data.AppResult
import com.samupdater.app.data.FirmwareRepository
import com.samupdater.app.data.prefs.SettingsStore
import com.samupdater.app.domain.FirmwareDecoder
import com.samupdater.app.domain.FirmwareInfo
import com.samupdater.app.domain.TrackedDevice
import com.samupdater.app.ui.Loadable
import com.samupdater.app.ui.myDeviceFlow
import com.samupdater.app.ui.toLoadable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RolloutRow(val csc: String, val country: String?, val build: String?, val date: String?, val hasNewest: Boolean)

data class UpdatesUiState(
    val tracked: TrackedDevice? = null,
    val firmware: Loadable<FirmwareInfo> = Loadable.Loading,
    val rollout: Loadable<List<RolloutRow>> = Loadable.Loading,
)

class UpdatesViewModel(
    private val repository: FirmwareRepository,
    settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(UpdatesUiState())
    val state: StateFlow<UpdatesUiState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        viewModelScope.launch { settings.myDeviceFlow().collect { load(it, force = false) } }
    }

    fun refresh() {
        _state.value.tracked?.let { load(it, force = true) }
    }

    private fun load(device: TrackedDevice, force: Boolean) {
        job?.cancel()
        _state.value = UpdatesUiState(tracked = device)
        job = viewModelScope.launch {
            val firmware = repository.firmware(device.model, device.csc, force).toLoadable()
            _state.update { it.copy(firmware = firmware) }
            _state.update { it.copy(rollout = loadRollout(device, force)) }
        }
    }

    private suspend fun loadRollout(device: TrackedDevice, force: Boolean): Loadable<List<RolloutRow>> {
        val catalog = repository.catalog()
        val countries = catalog.cscSuggestions.associate { it.code to it.country }
        val cscs = (listOf(device.csc) + RolloutCscs.forModel(device.model, catalog.deviceFor(device.model)?.rolloutCscs))
            .distinct()
        val entries = repository.rollout(device.model, cscs, force)
        val loaded = entries.mapNotNull { entry -> (entry.result as? AppResult.Success)?.let { entry.csc to it.value } }
        if (loaded.isEmpty()) return Loadable.Failed("Couldn't load rollout data right now.")

        val newest = loaded.mapNotNull { FirmwareDecoder.decode(it.second.latest.build) }.maxByOrNull { it.sortKey }
        val rows = loaded.map { (csc, info) ->
            RolloutRow(
                csc = csc,
                country = countries[csc],
                build = info.latest.build,
                date = info.latest.releaseDate,
                hasNewest = newest != null && info.latest.build.endsWith(newest.buildTag),
            )
        }
        return Loadable.Ready(rows.sortedWith(compareByDescending<RolloutRow> { it.hasNewest }.thenBy { it.csc }))
    }
}

/** Which regions to compare when the catalog doesn't list any for a model. */
object RolloutCscs {
    private val INTERNATIONAL = listOf("EUX", "DBT", "BTU", "INS", "XSA", "XME", "THL", "XID", "TGY", "ZTO")
    private val US_UNLOCKED = listOf("XAA", "XAC")
    private val US_CARRIER = listOf("TMB", "VZW", "ATT")
    private val KOREA = listOf("KOO")
    private val CHINA = listOf("CHC", "TGY", "BRI")

    fun forModel(model: String, fromCatalog: List<String>?): List<String> {
        if (!fromCatalog.isNullOrEmpty()) return fromCatalog
        return when {
            model.endsWith("U1") -> US_UNLOCKED
            model.endsWith("U") -> US_CARRIER
            model.endsWith("N") -> KOREA
            model.endsWith("0") -> CHINA
            else -> INTERNATIONAL
        }
    }
}
