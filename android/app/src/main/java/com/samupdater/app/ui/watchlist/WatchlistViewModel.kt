package com.samupdater.app.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samupdater.app.data.FirmwareRepository
import com.samupdater.app.data.prefs.SettingsStore
import com.samupdater.app.domain.CscOption
import com.samupdater.app.domain.FirmwareInfo
import com.samupdater.app.domain.TrackedDevice
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

/**
 * [isMine] marks the device picked on the Device tab. [isThisPhone] is only true when that device is also
 * the Galaxy phone the app runs on, so a model typed in by hand isn't called "this phone".
 */
data class WatchEntry(
    val device: TrackedDevice,
    val isMine: Boolean,
    val isThisPhone: Boolean,
    val firmware: Loadable<FirmwareInfo>,
)

data class WatchlistUiState(
    val entries: List<WatchEntry> = emptyList(),
    val cscOptions: List<CscOption> = emptyList(),
    val isFull: Boolean = false,
)

class WatchlistViewModel(
    private val repository: FirmwareRepository,
    private val settings: SettingsStore,
    private val detectedModel: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(WatchlistUiState())
    val state: StateFlow<WatchlistUiState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        viewModelScope.launch {
            _state.update { it.copy(cscOptions = repository.catalog().cscSuggestions) }
        }
        viewModelScope.launch {
            settings.settings
                .map { it.myDevice to it.watchlist }
                .distinctUntilChanged()
                .collect { (mine, watchlist) -> load(mine, watchlist, force = false) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val current = settings.current()
            load(current.myDevice, current.watchlist, force = true)
        }
    }

    fun add(device: TrackedDevice) {
        viewModelScope.launch { settings.addToWatchlist(device) }
    }

    fun remove(device: TrackedDevice) {
        viewModelScope.launch { settings.removeFromWatchlist(device.key) }
    }

    private fun load(mine: TrackedDevice?, watchlist: List<TrackedDevice>, force: Boolean) {
        job?.cancel()
        val devices = (listOfNotNull(mine) + watchlist).distinctBy { it.key }
        val previous = _state.value.entries.associateBy { it.device.key }
        _state.update { state ->
            state.copy(
                isFull = watchlist.size >= SettingsStore.MAX_WATCHLIST,
                entries = devices.map { device ->
                    val cached = previous[device.key]?.firmware?.takeIf { !force && it is Loadable.Ready }
                    val isMine = device.key == mine?.key
                    WatchEntry(
                        device = device,
                        isMine = isMine,
                        isThisPhone = isMine && device.model == detectedModel,
                        firmware = cached ?: Loadable.Loading,
                    )
                },
            )
        }
        job = viewModelScope.launch {
            devices.forEach { device ->
                launch {
                    val result = repository.firmware(device.model, device.csc, force).toLoadable()
                    _state.update { state ->
                        state.copy(entries = state.entries.map { if (it.device.key == device.key) it.copy(firmware = result) else it })
                    }
                }
            }
        }
    }
}
