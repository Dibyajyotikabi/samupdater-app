package com.samupdater.app.ui

import com.samupdater.app.data.prefs.SettingsStore
import com.samupdater.app.domain.TrackedDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/** The device every tab follows. Emits again only when the user changes it. */
fun SettingsStore.myDeviceFlow(): Flow<TrackedDevice> = settings.map { it.myDevice }.filterNotNull().distinctUntilChanged()
