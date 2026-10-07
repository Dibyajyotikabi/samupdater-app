package com.samupdater.app.data.fota

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

/** Parsed version.xml / version.test.xml from Samsung's FOTA server. */
data class FotaVersionInfo(
    val latest: String?,
    val androidCode: String?,
    val upgrades: List<String>,
) {
    /** In version.test.xml every entry is a hash (MD5, newer ones SHA-256). */
    val allEntries: List<String> get() = listOfNotNull(latest) + upgrades
}

object VersionXmlParser {

    fun parse(xml: String): FotaVersionInfo {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        }
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray()))
        val latest = doc.getElementsByTagName("latest").item(0) as? Element
        val values = doc.getElementsByTagName("value")
        val upgrades = (0 until values.length)
            .mapNotNull { values.item(it).textContent?.trim() }
            .filter { it.isNotEmpty() }
        return FotaVersionInfo(
            latest = latest?.textContent?.trim()?.takeIf { it.isNotEmpty() },
            androidCode = latest?.getAttribute("o")?.takeIf { it.isNotEmpty() },
            upgrades = upgrades,
        )
    }
}
