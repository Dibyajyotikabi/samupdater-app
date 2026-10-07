package com.samupdater.app.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Tiny time-based cache so switching tabs doesn't refetch everything. */
class MemoryCache<K : Any, V : Any>(
    private val ttlMillis: Long,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private data class Entry<V>(val value: V, val storedAt: Long)

    private val mutex = Mutex()
    private var entries: Map<K, Entry<V>> = emptyMap()

    suspend fun getOrLoad(key: K, force: Boolean = false, load: suspend () -> V): V {
        if (!force) {
            mutex.withLock { entries[key] }?.takeIf { clock() - it.storedAt < ttlMillis }?.let { return it.value }
        }
        val value = load()
        mutex.withLock { entries = entries + (key to Entry(value, clock())) }
        return value
    }
}
