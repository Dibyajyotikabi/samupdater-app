package com.samupdater.app.domain

import com.samupdater.app.data.fota.VersionXmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class TestBuildDecoderTest {

    private val decoder = TestBuildDecoder()
    private val stable = "S938BXXU4BYJ2/S938BOXM4BYJ2/S938BXXU4BYJ2"
    private val history = listOf(
        "S938BXXU1AYA1/S938BOXM1AYA1/S938BXXU1AYA1",
        "S938BXXU3BYE1/S938BOXM3BYE1/S938BXXU3BYE1",
    )

    private fun hash(algorithm: String, text: String) =
        MessageDigest.getInstance(algorithm).digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun md5(text: String) = hash("MD5", text)

    private fun fixture(name: String) = requireNotNull(javaClass.getResource("/fota/$name")) { "Missing $name" }.readText()

    @Test
    fun `finds next month, next major and beta builds behind their hashes`() {
        val hashes = listOf(
            md5("S938BXXS4BYK1/S938BOXM4BYK1/S938BXXS4BYK1"),
            md5("S938BXXU4CZA3/S938BOXM4CZA3/S938BXXU4CZA3"),
            md5("S938BXXU4ZYL5/S938BOXM4ZYL5/S938BXXU4ZYL5"),
        )

        val result = decoder.decode(stable, hashes)

        assertEquals(3, result.totalHashes)
        assertEquals(0, result.undecodedCount)
        assertEquals("S938BXXU4CZA3", result.latest?.raw)
        assertTrue(result.decoded.any { it.isBeta })
        assertEquals(3, result.inTesting.size)
    }

    @Test
    fun `decodes a beta whose modem keeps the stable One UI letter`() {
        val result = decoder.decode(stable, listOf(md5("S938BXXU4ZYL5/S938BOXM4ZYL5/S938BXXU4BYL5")))

        assertEquals("S938BXXU4ZYL5", result.decoded.single().raw)
    }

    @Test
    fun `decodes a build whose modem is a few revisions behind`() {
        val result = decoder.decode(stable, listOf(md5("S938BXXU4BYK7/S938BOXM4BYK7/S938BXXU4BYK5")))

        assertEquals("S938BXXU4BYK7", result.decoded.single().raw)
    }

    @Test
    fun `finds older test builds from the stable history but keeps them out of in testing`() {
        val result = decoder.decode(stable, listOf(md5("S938BXXU2AYC3/S938BOXM2AYC3/S938BXXU2AYC3")), history)

        assertEquals("S938BXXU2AYC3", result.decoded.single().raw)
        assertTrue(result.inTesting.isEmpty())
        assertEquals(null, result.latestInTesting)
    }

    @Test
    fun `matches SHA-256 hashes too`() {
        val result = decoder.decode(stable, listOf(hash("SHA-256", "S938BXXU4BYL2/S938BOXM4BYL2/S938BXXU4BYL2")))

        assertEquals(1, result.totalHashes)
        assertEquals("S938BXXU4BYL2", result.decoded.single().raw)
    }

    @Test
    fun `handles Wi-Fi models without a modem part`() {
        val result = decoder.decode("X710XXU4BYJ2/X710OXM4BYJ2/", listOf(md5("X710XXU4BYK1/X710OXM4BYK1/")))

        assertEquals("X710XXU4BYK1", result.decoded.single().raw)
    }

    @Test
    fun `counts hashes it cannot decode`() {
        val result = decoder.decode(stable, listOf(md5("something else"), "not-a-hash"))

        assertEquals(1, result.totalHashes)
        assertEquals(1, result.undecodedCount)
        assertEquals(null, result.latest)
    }

    @Test
    fun `returns nothing for an unreadable stable build`() {
        val result = decoder.decode("garbage", listOf(md5("x")))
        assertTrue(result.decoded.isEmpty())
    }

    @Test
    fun `decodes most of the real Galaxy S25 Ultra test list`() {
        val stableInfo = VersionXmlParser.parse(fixture("S938B_INS_stable.xml"))
        val tests = VersionXmlParser.parse(fixture("S938B_INS_test.xml"))

        val result = decoder.decode(requireNotNull(stableInfo.latest), tests.allEntries, stableInfo.allEntries)

        assertEquals(REAL_HASHES, result.totalHashes)
        assertTrue("decoded only ${result.matchedHashes}", result.matchedHashes >= MIN_REAL_DECODED)
    }

    private companion object {
        /** 168 are known to be decodable. The two the decoder skips use a month-old or U/S-swapped modem. */
        const val MIN_REAL_DECODED = 160
        const val REAL_HASHES = 207
    }
}
