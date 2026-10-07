package com.samupdater.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirmwareDecoderTest {

    @Test
    fun `decodes a European S24 Ultra security build`() {
        val build = decoded("S928BXXS4AXK3")

        assertEquals("S928B", build.modelCode)
        assertEquals("XX", build.region)
        assertEquals(UpdateType.SECURITY, build.updateType)
        assertEquals(4, build.bootloader)
        assertEquals('A', build.oneUiIteration)
        assertEquals(2024, build.year)
        assertEquals(11, build.month)
        assertEquals(3, build.buildIteration)
        assertEquals(0, build.majorUpgradeIndex)
    }

    @Test
    fun `decodes letter bootloaders and 2026 year letters`() {
        val build = decoded("S938BXXSCCZH1")

        assertEquals(12, build.bootloader)
        assertEquals(2026, build.year)
        assertEquals(8, build.month)
        assertEquals(2, build.majorUpgradeIndex)
    }

    @Test
    fun `decodes a Chinese build with a numeric model suffix`() {
        val build = decoded("S9280ZCS6DZI1")

        assertEquals("S9280", build.modelCode)
        assertEquals("ZC", build.region)
        assertEquals(6, build.bootloader)
        assertEquals(9, build.month)
    }

    @Test
    fun `flags One UI beta builds by the Z marker`() {
        val beta = decoded("S938BXXU4ZYJ1")

        assertTrue(beta.isBeta)
        assertNull(beta.majorUpgradeIndex)
        assertFalse(decoded("S938BXXU4BYJ2").isBeta)
    }

    @Test
    fun `takes the PDA from a full triple and ignores case and spaces`() {
        val build = decoded("  s938bxxu4byj2/S938BOXM4BYJ2/S938BXXU4BYJ2 ")
        assertEquals("S938BXXU4BYJ2", build.raw)
    }

    @Test
    fun `rejects strings that are not builds`() {
        assertNull(FirmwareDecoder.decode(""))
        assertNull(FirmwareDecoder.decode("hello world"))
        assertNull(FirmwareDecoder.decode("S938BXX"))
        assertNull(FirmwareDecoder.decode("S938BXXU4BYM2")) // M is not a month
        assertNull(FirmwareDecoder.decode("S938BXXU04YJ2")) // 0 is not a bootloader
    }

    @Test
    fun `newer builds sort after older ones`() {
        val older = decoded("S928BXXS4AXK3")
        val newerMonth = decoded("S928BXXS4AXL1")
        val majorUpgrade = decoded("S928BXXU4BXJ1")
        val newBootloader = decoded("S928BXXU5AXA1")

        assertTrue(newerMonth.isNewerThan(older))
        assertTrue(majorUpgrade.isNewerThan(newerMonth))
        assertTrue(newBootloader.isNewerThan(majorUpgrade))
    }

    @Test
    fun `year and month letters round trip`() {
        for (year in 2015..2030) assertEquals(year, FirmwareDecoder.yearFromLetter(checkNotNull(FirmwareDecoder.letterForYear(year))))
        for (month in 1..12) assertEquals(month, FirmwareDecoder.monthFromLetter(checkNotNull(FirmwareDecoder.letterForMonth(month))))
        assertEquals('X', FirmwareDecoder.letterForYear(2024))
        assertEquals('A', FirmwareDecoder.letterForYear(2027))
    }

    @Test
    fun `explanation covers every part of the build`() {
        val parts = BuildExplanation.explain(decoded("S928BXXS4AXK3"))

        assertEquals(8, parts.size)
        assertEquals("S928BXXS4AXK3", parts.joinToString("") { it.chars })
        assertEquals("November", parts.first { it.title == "Month" }.meaning)
    }

    private fun decoded(raw: String): FirmwareBuild =
        requireNotNull(FirmwareDecoder.decode(raw)) { "Expected $raw to decode" }
}
