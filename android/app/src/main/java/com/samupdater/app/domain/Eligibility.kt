package com.samupdater.app.domain

data class EligibilityStatus(
    val upgradesPromised: Int,
    val upgradesUsed: Int,
    val nextAndroid: Int?,
    val nextOneUi: String?,
) {
    val upgradesLeft: Int get() = (upgradesPromised - upgradesUsed).coerceAtLeast(0)
    val isEligibleForNext: Boolean get() = upgradesLeft > 0
}

object EligibilityCalculator {

    /** One UI major versions shipped with each Android release. Point releases like 8.5 stay on the same Android. */
    private val ONE_UI_FOR_ANDROID = mapOf(
        11 to "3", 12 to "4", 13 to "5", 14 to "6", 15 to "7", 16 to "8", 17 to "9", 18 to "10",
    )

    fun calculate(launchAndroid: Int, currentAndroid: Int, promisedUpgrades: Int): EligibilityStatus {
        val used = (currentAndroid - launchAndroid).coerceAtLeast(0)
        val left = promisedUpgrades - used
        val nextAndroid = if (left > 0) currentAndroid + 1 else null
        return EligibilityStatus(
            upgradesPromised = promisedUpgrades,
            upgradesUsed = used,
            nextAndroid = nextAndroid,
            nextOneUi = nextAndroid?.let { ONE_UI_FOR_ANDROID[it] },
        )
    }

    /** "Android 16" -> 16. */
    fun parseAndroidVersion(text: String?): Int? =
        text?.let { Regex("(\\d+)").find(it)?.groupValues?.get(1)?.toIntOrNull() }
}
