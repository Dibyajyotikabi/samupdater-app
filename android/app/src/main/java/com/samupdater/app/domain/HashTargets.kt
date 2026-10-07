package com.samupdater.app.domain

import java.security.MessageDigest

/**
 * The hashes from version.test.xml, split by algorithm. Older entries are MD5 (32 hex characters), newer
 * ones SHA-256 (64). Each candidate is only hashed with the algorithms that are actually in the list.
 */
internal class HashTargets(hashes: Collection<String>) {

    private val md5Targets: Set<String>
    private val sha256Targets: Set<String>

    init {
        val clean = hashes.map { it.trim().lowercase() }.filter { HEX.matches(it) }
        md5Targets = clean.filter { it.length == MD5_HEX_LENGTH }.toSet()
        sha256Targets = clean.filter { it.length == SHA256_HEX_LENGTH }.toSet()
    }

    private val md5 = MessageDigest.getInstance("MD5").takeIf { md5Targets.isNotEmpty() }
    private val sha256 = MessageDigest.getInstance("SHA-256").takeIf { sha256Targets.isNotEmpty() }

    val size: Int get() = md5Targets.size + sha256Targets.size
    val isEmpty: Boolean get() = size == 0

    /** Returns the listed hash that [text] produces, or null if it isn't one of them. */
    fun match(text: String): String? {
        val bytes = text.toByteArray()
        md5?.let { digest -> toHex(digest.digest(bytes)).takeIf { it in md5Targets }?.let { return it } }
        sha256?.let { digest -> toHex(digest.digest(bytes)).takeIf { it in sha256Targets }?.let { return it } }
        return null
    }

    private fun toHex(bytes: ByteArray): String {
        val chars = CharArray(bytes.size * 2)
        bytes.forEachIndexed { i, byte ->
            val value = byte.toInt() and BYTE_MASK
            chars[i * 2] = HEX_DIGITS[value ushr NIBBLE_BITS]
            chars[i * 2 + 1] = HEX_DIGITS[value and NIBBLE_MASK]
        }
        return String(chars)
    }

    private companion object {
        const val MD5_HEX_LENGTH = 32
        const val SHA256_HEX_LENGTH = 64
        const val BYTE_MASK = 0xFF
        const val NIBBLE_MASK = 0x0F
        const val NIBBLE_BITS = 4
        const val HEX_DIGITS = "0123456789abcdef"
        val HEX = Regex("^[0-9a-f]+$")
    }
}
