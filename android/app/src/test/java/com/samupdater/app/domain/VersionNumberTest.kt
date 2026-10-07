package com.samupdater.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VersionNumberTest {

    @Test
    fun `drops a trailing zero so a major release shows as one digit`() {
        assertEquals("9", VersionNumber.from("One UI 9.0"))
    }

    @Test
    fun `keeps point releases`() {
        assertEquals("8.5", VersionNumber.from("8.5"))
    }

    @Test
    fun `reads a bare major version`() {
        assertEquals("7", VersionNumber.from("One UI 7"))
    }

    @Test
    fun `returns null when there is no number`() {
        assertNull(VersionNumber.from(null))
        assertNull(VersionNumber.from("One UI"))
    }

    @Test
    fun `strips the word Android from an Android version`() {
        assertEquals("17", VersionNumber.from("Android 17"))
    }
}
