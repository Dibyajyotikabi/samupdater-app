package com.samupdater.app.ui.beta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samupdater.app.data.FirmwareRepository
import com.samupdater.app.data.prefs.SettingsStore
import com.samupdater.app.domain.BetaProgram
import com.samupdater.app.domain.TestBuildDecoder
import com.samupdater.app.domain.TrackedDevice
import com.samupdater.app.domain.UpcomingUpdate
import com.samupdater.app.ui.Loadable
import com.samupdater.app.ui.myDeviceFlow
import com.samupdater.app.ui.toLoadable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TestBuildSummary(val result: TestBuildDecoder.Result) {
    /** A build in testing with a newer One UI letter (or a Z beta) means the next major update is in testing. */
    val nextMajorInTesting: Boolean
        get() {
            val stableIndex = result.stable?.majorUpgradeIndex
            return result.inTesting.any { build ->
                build.isBeta || stableIndex == null || (build.majorUpgradeIndex ?: 0) > stableIndex
            }
        }
}

data class BetaUiState(
    val tracked: TrackedDevice? = null,
    val programs: Loadable<List<BetaProgram>> = Loadable.Loading,
    val upcoming: List<UpcomingUpdate> = emptyList(),
    val tests: Loadable<TestBuildSummary> = Loadable.Loading,
)

class BetaViewModel(
    private val repository: FirmwareRepository,
    settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(BetaUiState())
    val state: StateFlow<BetaUiState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        viewModelScope.launch { settings.myDeviceFlow().collect { load(it, force = false) } }
    }

    fun refresh() {
        _state.value.tracked?.let { load(it, force = true) }
    }

    private fun load(device: TrackedDevice, force: Boolean) {
        job?.cancel()
        _state.value = BetaUiState(tracked = device)
        job = viewModelScope.launch {
            val catalog = repository.catalog(force)
            _state.update {
                it.copy(programs = Loadable.Ready(catalog.betaFor(device.model)), upcoming = catalog.upcoming)
            }
            val tests = repository.testBuilds(device.model, device.csc, force).toLoadable()
            _state.update { current ->
                current.copy(
                    tests = when (tests) {
                        is Loadable.Ready -> Loadable.Ready(TestBuildSummary(tests.value))
                        is Loadable.Failed -> tests
                        Loadable.Loading -> Loadable.Loading
                    },
                )
            }
        }
    }
}
