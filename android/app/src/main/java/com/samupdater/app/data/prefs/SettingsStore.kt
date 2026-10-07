package com.samupdater.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.samupdater.app.domain.TrackedDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AlertSettings(val stable: Boolean = true, val beta: Boolean = true, val test: Boolean = true)

data class Settings(
    val myDevice: TrackedDevice? = null,
    val watchlist: List<TrackedDevice> = emptyList(),
    val alerts: AlertSettings = AlertSettings(),
    val lastSeen: Map<String, String> = emptyMap(),
    val onboarded: Boolean = false,
) {
    /** My device first, then the watchlist, without duplicates. */
    val allTracked: List<TrackedDevice>
        get() = (listOfNotNull(myDevice) + watchlist).distinctBy { it.key }
}

class SettingsStore(context: Context, private val json: Json) {
    private val store = context.applicationContext.dataStore
    private val deviceListSerializer = ListSerializer(TrackedDevice.serializer())
    private val lastSeenSerializer = MapSerializer(String.serializer(), String.serializer())

    val settings: Flow<Settings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map(::toSettings)

    suspend fun current(): Settings = settings.first()

    suspend fun setMyDevice(device: TrackedDevice) = store.edit {
        it[MY_DEVICE] = json.encodeToString(TrackedDevice.serializer(), device)
        it[ONBOARDED] = true
    }

    suspend fun addToWatchlist(device: TrackedDevice) = updateWatchlist { list ->
        (list.filterNot { it.key == device.key } + device).take(MAX_WATCHLIST)
    }

    suspend fun removeFromWatchlist(key: String) = updateWatchlist { list -> list.filterNot { it.key == key } }

    suspend fun setAlerts(alerts: AlertSettings) = store.edit {
        it[ALERT_STABLE] = alerts.stable
        it[ALERT_BETA] = alerts.beta
        it[ALERT_TEST] = alerts.test
    }

    /** Remembers the newest build we told the user about, per device and channel. */
    suspend fun setLastSeen(updates: Map<String, String>) = store.edit { prefs ->
        val existing = decodeOr(prefs[LAST_SEEN], lastSeenSerializer, emptyMap())
        prefs[LAST_SEEN] = json.encodeToString(lastSeenSerializer, existing + updates)
    }

    private suspend fun updateWatchlist(transform: (List<TrackedDevice>) -> List<TrackedDevice>) = store.edit { prefs ->
        val list = decodeOr(prefs[WATCHLIST], deviceListSerializer, emptyList())
        prefs[WATCHLIST] = json.encodeToString(deviceListSerializer, transform(list))
    }

    private fun toSettings(prefs: Preferences) = Settings(
        myDevice = prefs[MY_DEVICE]?.let { decodeOr(it, TrackedDevice.serializer().nullable, null) },
        watchlist = decodeOr(prefs[WATCHLIST], deviceListSerializer, emptyList()),
        alerts = AlertSettings(
            stable = prefs[ALERT_STABLE] ?: true,
            beta = prefs[ALERT_BETA] ?: true,
            test = prefs[ALERT_TEST] ?: true,
        ),
        lastSeen = decodeOr(prefs[LAST_SEEN], lastSeenSerializer, emptyMap()),
        onboarded = prefs[ONBOARDED] ?: false,
    )

    private fun <T> decodeOr(text: String?, serializer: kotlinx.serialization.KSerializer<T>, fallback: T): T {
        if (text.isNullOrBlank()) return fallback
        return try {
            json.decodeFromString(serializer, text)
        } catch (e: Exception) {
            android.util.Log.w("SamUpdater", "Stored settings were unreadable, using defaults", e)
            fallback
        }
    }

    companion object {
        const val MAX_WATCHLIST = 20
        private val MY_DEVICE = stringPreferencesKey("my_device")
        private val WATCHLIST = stringPreferencesKey("watchlist")
        private val LAST_SEEN = stringPreferencesKey("last_seen")
        private val ONBOARDED = booleanPreferencesKey("onboarded")
        private val ALERT_STABLE = booleanPreferencesKey("alert_stable")
        private val ALERT_BETA = booleanPreferencesKey("alert_beta")
        private val ALERT_TEST = booleanPreferencesKey("alert_test")
    }
}
