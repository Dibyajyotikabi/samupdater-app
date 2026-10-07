package com.samupdater.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelsTest {

    @Test
    fun `update status compares installed and latest builds`() {
        assertEquals(UpdateState.UP_TO_DATE, UpdateStatus.compare("S938BXXU4BYJ2", "S938BXXU4BYJ2"))
        assertEquals(UpdateState.UPDATE_AVAILABLE, UpdateStatus.compare("S938BXXU4BYJ2", "S938BXXS4BYK1"))
        assertEquals(UpdateState.ON_BETA, UpdateStatus.compare("S938BXXU4ZYJ1", "S938BXXU4BYJ2"))
        assertEquals(UpdateState.UP_TO_DATE, UpdateStatus.compare("S938BXXS4BYK1", "S938BXXU4BYJ2"))
        assertEquals(UpdateState.UNKNOWN, UpdateStatus.compare(null, "S938BXXU4BYJ2"))
        assertEquals(UpdateState.UNKNOWN, UpdateStatus.compare("S938BXXU4BYJ2", "nope"))
    }

    @Test
    fun `catalog picks the longest matching model prefix`() {
        val catalog = Catalog(
            devices = listOf(
                CatalogDevice(modelPrefix = "SM-S93", name = "Galaxy S25 series", launchAndroid = 15, osUpgrades = 7),
                CatalogDevice(modelPrefix = "SM-S938", name = "Galaxy S25 Ultra", launchAndroid = 15, osUpgrades = 7),
            ),
        )

        assertEquals("Galaxy S25 Ultra", catalog.deviceFor("SM-S938B")?.name)
        assertEquals("Galaxy S25 series", catalog.deviceFor("SM-S931B")?.name)
        assertNull(catalog.deviceFor("SM-A566B"))
    }

    @Test
    fun `catalog finds beta programs by prefix`() {
        val catalog = Catalog(
            betaPrograms = listOf(
                BetaProgram(oneUi = "9", status = "open", modelPrefixes = listOf("SM-S93"), countries = listOf("India")),
            ),
        )

        assertEquals(1, catalog.betaFor("SM-S938B").size)
        assertEquals(0, catalog.betaFor("SM-S928B").size)
    }

    @Test
    fun `tracked device label prefers the nickname`() {
        assertEquals("Mom's phone", TrackedDevice("SM-A566B", "INS", "Mom's phone").label)
        assertEquals("SM-A566B", TrackedDevice("SM-A566B", "INS", " ").label)
    }
}
