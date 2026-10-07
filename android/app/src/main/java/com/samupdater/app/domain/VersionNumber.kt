package com.samupdater.app.domain

/**
 * Pulls the short number out of version labels like "One UI 9.0", "8.5" or "Android 17",
 * so the UI can add its own prefix without doubling words.
 */
object VersionNumber {
    private val NUMBER = Regex("""\d+(\.\d+)?""")

    fun from(label: String?): String? {
        val number = label?.let { NUMBER.find(it)?.value } ?: return null
        return number.removeSuffix(".0")
    }
}
