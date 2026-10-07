package com.samupdater.app.data.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceInfoReaderTest {

    @Test
    fun `formats the One UI system property`() {
        assertEquals("8.5", DeviceInfoReader.formatOneUi("80500"))
        assertEquals("7", DeviceInfoReader.formatOneUi("70000"))
        assertEquals("6.1", DeviceInfoReader.formatOneUi(" 60100 "))
        assertNull(DeviceInfoReader.formatOneUi("0"))
        assertNull(DeviceInfoReader.formatOneUi("abc"))
        assertNull(DeviceInfoReader.formatOneUi(""))
    }

    @Test
    fun `maps SDK levels to Android versions`() {
        assertEquals(8, DeviceInfoReader.androidFromSdk(26))
        assertEquals(9, DeviceInfoReader.androidFromSdk(28))
        assertEquals(11, DeviceInfoReader.androidFromSdk(30))
        assertEquals(12, DeviceInfoReader.androidFromSdk(31))
        assertEquals(12, DeviceInfoReader.androidFromSdk(32))
        assertEquals(13, DeviceInfoReader.androidFromSdk(33))
        assertEquals(15, DeviceInfoReader.androidFromSdk(35))
        assertEquals(16, DeviceInfoReader.androidFromSdk(36))
    }
}
