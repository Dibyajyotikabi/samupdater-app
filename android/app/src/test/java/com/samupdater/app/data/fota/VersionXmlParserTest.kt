package com.samupdater.app.data.fota

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionXmlParserTest {

    @Test
    fun `reads latest build, Android code and history`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <versioninfo>
              <url>https://fota-cloud-dn.ospserver.net/firmware/</url>
              <firmware>
                <model>SM-S938B</model>
                <cc>INS</cc>
                <version>
                  <latest o="16">S938BXXU4BYJ2/S938BOXM4BYJ2/S938BXXU4BYJ2</latest>
                  <upgrade>
                    <value rcount="1" fwsize="123">S938BXXS3BYI1/S938BOXM3BYI1/S938BXXS3BYI1</value>
                    <value rcount="1" fwsize="456">S938BXXU3BYH4/S938BOXM3BYH4/S938BXXU3BYH4</value>
                  </upgrade>
                </version>
              </firmware>
            </versioninfo>
        """.trimIndent()

        val info = VersionXmlParser.parse(xml)

        assertEquals("S938BXXU4BYJ2/S938BOXM4BYJ2/S938BXXU4BYJ2", info.latest)
        assertEquals("16", info.androidCode)
        assertEquals(2, info.upgrades.size)
        assertEquals(3, info.allEntries.size)
    }

    @Test
    fun `handles a test file with only hashes and an empty latest tag`() {
        val xml = """
            <versioninfo><firmware><version>
              <latest></latest>
              <upgrade>
                <value>0123456789abcdef0123456789abcdef</value>
                <value> </value>
              </upgrade>
            </version></firmware></versioninfo>
        """.trimIndent()

        val info = VersionXmlParser.parse(xml)

        assertNull(info.latest)
        assertNull(info.androidCode)
        assertEquals(listOf("0123456789abcdef0123456789abcdef"), info.upgrades)
    }

    @Test
    fun `refuses documents with a DOCTYPE`() {
        val xml = """<?xml version="1.0"?><!DOCTYPE x [<!ENTITY e "boom">]><versioninfo><latest>&e;</latest></versioninfo>"""
        assertTrue(runCatching { VersionXmlParser.parse(xml) }.isFailure)
    }
}
