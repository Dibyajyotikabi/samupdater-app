package com.samupdater.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangeDetectorTest {

    private val phone = TrackedDevice("SM-S938B", "INS")

    @Test
    fun `first sighting is stored silently`() {
        val observed = listOf(Observation(phone, Channel.STABLE, "S938BXXU4BYJ2"))
        assertTrue(ChangeDetector.detect(emptyMap(), observed).isEmpty())
    }

    @Test
    fun `reports only values that changed`() {
        val lastSeen = mapOf(
            "SM-S938B|INS|STABLE" to "S938BXXU4BYJ2",
            "SM-S938B|INS|BETA" to "S938BXXU4ZYJ1",
        )
        val observed = listOf(
            Observation(phone, Channel.STABLE, "S938BXXU4BYK1"),
            Observation(phone, Channel.BETA, "S938BXXU4ZYJ1"),
        )

        val changes = ChangeDetector.detect(lastSeen, observed)

        assertEquals(listOf(Channel.STABLE), changes.map { it.channel })
    }

    @Test
    fun `snapshot keys by device and channel`() {
        val snapshot = ChangeDetector.snapshot(listOf(Observation(phone, Channel.TEST, "abc")))
        assertEquals(mapOf("SM-S938B|INS|TEST" to "abc"), snapshot)
    }
}
