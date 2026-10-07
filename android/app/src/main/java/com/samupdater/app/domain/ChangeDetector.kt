package com.samupdater.app.domain

enum class Channel { STABLE, BETA, TEST }

data class Observation(val device: TrackedDevice, val channel: Channel, val value: String) {
    val key: String get() = "${device.key}|${channel.name}"
}

/** Finds builds the user hasn't been told about yet. The first sighting of a device is stored silently. */
object ChangeDetector {
    fun detect(lastSeen: Map<String, String>, observed: List<Observation>): List<Observation> =
        observed.filter { obs ->
            val previous = lastSeen[obs.key]
            previous != null && previous != obs.value
        }

    fun snapshot(observed: List<Observation>): Map<String, String> = observed.associate { it.key to it.value }
}
