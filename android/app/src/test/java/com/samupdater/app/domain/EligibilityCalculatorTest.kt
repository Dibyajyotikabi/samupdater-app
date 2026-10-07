package com.samupdater.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EligibilityCalculatorTest {

    @Test
    fun `S24 on Android 16 has five upgrades left and gets One UI 9 next`() {
        val status = EligibilityCalculator.calculate(launchAndroid = 14, currentAndroid = 16, promisedUpgrades = 7)

        assertEquals(2, status.upgradesUsed)
        assertEquals(5, status.upgradesLeft)
        assertEquals(17, status.nextAndroid)
        assertEquals("9", status.nextOneUi)
        assertTrue(status.isEligibleForNext)
    }

    @Test
    fun `device that used every promised upgrade is not eligible`() {
        val status = EligibilityCalculator.calculate(launchAndroid = 12, currentAndroid = 16, promisedUpgrades = 4)

        assertFalse(status.isEligibleForNext)
        assertNull(status.nextAndroid)
        assertNull(status.nextOneUi)
    }

    @Test
    fun `current older than launch never counts negative upgrades`() {
        val status = EligibilityCalculator.calculate(launchAndroid = 15, currentAndroid = 14, promisedUpgrades = 7)
        assertEquals(0, status.upgradesUsed)
    }

    @Test
    fun `parses Android versions from text`() {
        assertEquals(16, EligibilityCalculator.parseAndroidVersion("Android 16"))
        assertEquals(15, EligibilityCalculator.parseAndroidVersion("15"))
        assertNull(EligibilityCalculator.parseAndroidVersion("unknown"))
        assertNull(EligibilityCalculator.parseAndroidVersion(null))
    }
}
