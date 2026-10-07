package com.samupdater.app.domain

/**
 * Which build suffixes are worth trying for each month, worked out from the model's stable history.
 *
 * version.test.xml keeps old test builds too (back to before launch), so we can't only look forward from
 * today's stable. Trying every bootloader and One UI letter for every month is far too slow on a phone,
 * so each month only gets the values that were current around then, with a little room either side.
 */
internal class TestBuildSearchSpace(history: List<FirmwareBuild>) {

    /** Stable builds only, oldest first. Betas would drag the One UI letter range up to Z. */
    private val releases = history.filterNot { it.isBeta }.sortedBy { it.monthIndex }

    val isEmpty: Boolean get() = releases.isEmpty()

    /** Months to search, as year * 12 + (month - 1). */
    val months: IntRange
        get() = if (isEmpty) IntRange.EMPTY
        else releases.first().monthIndex - MONTHS_BEFORE_LAUNCH..releases.last().monthIndex + MONTHS_AHEAD

    fun bootloaders(month: Int): IntRange {
        val low = (current(month).bootloader - BOOTLOADER_LAG).coerceAtLeast(1)
        val high = upcoming(month).maxOf { it.bootloader } + 1
        return low..high
    }

    /** Stable One UI letters that could be in testing that month. Betas (Z) are added by the decoder. */
    fun oneUiLetters(month: Int): List<Char> {
        val low = current(month).oneUiIteration
        val high = minOf(upcoming(month).maxOf { it.oneUiIteration } + 1, LAST_STABLE_LETTER)
        return (low..high).toList()
    }

    /** The newest release out by [month], or the first one for months before launch. */
    private fun current(month: Int): FirmwareBuild = releases.lastOrNull { it.monthIndex <= month } ?: releases.first()

    /** Releases out by a few months after [month]. Test builds run ahead of the stable they become. */
    private fun upcoming(month: Int): List<FirmwareBuild> =
        releases.filter { it.monthIndex <= month + LOOK_AHEAD_MONTHS }.ifEmpty { releases.take(1) }

    companion object {
        /** Betas often stay on an older bootloader than the stable that shipped in the meantime. */
        private const val BOOTLOADER_LAG = 2
        private const val LOOK_AHEAD_MONTHS = 3
        private const val MONTHS_BEFORE_LAUNCH = 3
        private const val MONTHS_AHEAD = 12
        private const val LAST_STABLE_LETTER = 'Y'
        const val MONTHS_PER_YEAR = 12
    }
}

internal val FirmwareBuild.monthIndex: Int get() = year * TestBuildSearchSpace.MONTHS_PER_YEAR + (month - 1)
