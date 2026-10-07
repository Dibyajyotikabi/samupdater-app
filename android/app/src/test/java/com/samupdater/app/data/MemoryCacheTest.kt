package com.samupdater.app.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryCacheTest {

    private var now = 0L
    private val cache = MemoryCache<String, Int>(ttlMillis = 1_000, clock = { now })
    private var loads = 0
    private val loader: suspend () -> Int = { ++loads }

    @Test
    fun `serves cached values until they expire`() = runTest {
        assertEquals(1, cache.getOrLoad("a", load = loader))
        now = 999
        assertEquals(1, cache.getOrLoad("a", load = loader))
        now = 1_000
        assertEquals(2, cache.getOrLoad("a", load = loader))
    }

    @Test
    fun `force skips the cache and keys are separate`() = runTest {
        cache.getOrLoad("a", load = loader)
        assertEquals(2, cache.getOrLoad("a", force = true, load = loader))
        assertEquals(3, cache.getOrLoad("b", load = loader))
        assertEquals(2, cache.getOrLoad("a", load = loader))
    }

    @Test
    fun `failed loads are not cached`() = runTest {
        runCatching { cache.getOrLoad("a") { error("offline") } }
        assertEquals(1, cache.getOrLoad("a", load = loader))
    }
}
