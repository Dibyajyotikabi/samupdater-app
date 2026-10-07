package com.samupdater.app.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samupdater.app.data.FirmwareRepository
import com.samupdater.app.data.getOrNull
import com.samupdater.app.data.device.DeviceInfoReader
import com.samupdater.app.data.prefs.SettingsStore
import com.samupdater.app.domain.Catalog
import com.samupdater.app.domain.CatalogDevice
import com.samupdater.app.domain.DeviceProfile
import com.samupdater.app.domain.EligibilityCalculator
import com.samupdater.app.domain.EligibilityStatus
import com.samupdater.app.domain.FirmwareInfo
import com.samupdater.app.domain.NotesBlock
import com.samupdater.app.domain.TrackedDevice
import com.samupdater.app.domain.UpcomingUpdate
import com.samupdater.app.domain.UpdateState
import com.samupdater.app.domain.UpdateStatus
import com.samupdater.app.ui.Loadable
import com.samupdater.app.ui.toLoadable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceUiState(
    val profile: DeviceProfile? = null,
    val tracked: TrackedDevice? = null,
    val needsSetup: Boolean = false,
    val firmware: Loadable<FirmwareInfo> = Loadable.Loading,
    val catalog: Catalog = Catalog(),
    val catalogDevice: CatalogDevice? = null,
    val eligibility: EligibilityStatus? = null,
    val updateState: UpdateState = UpdateState.UNKNOWN,
    val upcoming: List<UpcomingUpdate> = emptyList(),
    val notes: List<NotesBlock> = emptyList(),
) {
    val isThisPhone: Boolean get() = profile?.model != null && profile.model == tracked?.model
}

class DeviceViewModel(
    private val repository: FirmwareRepository,
    private val settings: SettingsStore,
    deviceInfo: DeviceInfoReader,
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceUiState())
    val state: StateFlow<DeviceUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            // Read the profile first so autoSetup and isThisPhone see it, without blocking the first frame.
            val profile = deviceInfo.read()
            _state.update { it.copy(profile = profile) }
            settings.settings.map { it.myDevice }.distinctUntilChanged().collect { tracked ->
                if (tracked == null) autoSetup() else load(tracked, force = false)
            }
        }
    }

    fun refresh() {
        _state.value.tracked?.let { load(it, force = true) }
    }

    fun saveDevice(device: TrackedDevice) {
        viewModelScope.launch { settings.setMyDevice(device) }
    }

    private suspend fun autoSetup() {
        val profile = _state.value.profile
        val model = profile?.model
        val csc = profile?.csc
        if (model != null && csc != null) {
            settings.setMyDevice(TrackedDevice(model, csc))
        } else {
            _state.update { it.copy(needsSetup = true, catalog = repository.catalog()) }
        }
    }

    private fun load(tracked: TrackedDevice, force: Boolean) {
        loadJob?.cancel()
        _state.update { it.copy(tracked = tracked, needsSetup = false, firmware = Loadable.Loading, notes = emptyList()) }
        loadJob = viewModelScope.launch {
            val catalog = repository.catalog(force)
            val firmware = repository.firmware(tracked.model, tracked.csc, force).toLoadable()
            _state.update { current -> buildState(current, catalog, firmware) }
            val notes = (firmware as? Loadable.Ready)?.value?.let { loadNotes(it, force) }.orEmpty()
            _state.update { it.copy(notes = notes) }
        }
    }

    /** Empty when Samsung's page is down. The card then falls back to the site's flat notes. */
    private suspend fun loadNotes(info: FirmwareInfo, force: Boolean): List<NotesBlock> {
        val url = info.sourceUrl ?: return emptyList()
        return repository.releaseNotes(url, info.latest.build, force).getOrNull().orEmpty()
    }

    private fun buildState(current: DeviceUiState, catalog: Catalog, firmware: Loadable<FirmwareInfo>): DeviceUiState {
        val tracked = current.tracked ?: return current
        val info = (firmware as? Loadable.Ready)?.value
        val catalogDevice = catalog.deviceFor(tracked.model)
        val currentAndroid = if (current.isThisPhone) current.profile?.androidVersion
        else EligibilityCalculator.parseAndroidVersion(info?.latest?.android)
        val eligibility = if (catalogDevice != null && currentAndroid != null) {
            EligibilityCalculator.calculate(catalogDevice.launchAndroid, currentAndroid, catalogDevice.osUpgrades)
        } else null
        val installed = if (current.isThisPhone) current.profile?.pdaBuild else null
        return current.copy(
            firmware = firmware,
            catalog = catalog,
            catalogDevice = catalogDevice,
            eligibility = eligibility,
            updateState = UpdateStatus.compare(installed, info?.latest?.build),
            upcoming = catalog.upcoming,
        )
    }
}
