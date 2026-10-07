package com.samupdater.app.domain

/**
 * A decoded Samsung PDA build string such as S938BXXSCCZH1.
 *
 * Layout (read from the end): [model code][region x2][type][bootloader][One UI][year][month][build].
 */
data class FirmwareBuild(
    val raw: String,
    val modelCode: String,
    val region: String,
    val updateType: UpdateType,
    val bootloader: Int,
    val oneUiIteration: Char,
    val year: Int,
    val month: Int,
    val buildIteration: Int,
) {
    val isBeta: Boolean get() = oneUiIteration == BETA_MARKER

    /**
     * 0 for the launch One UI version, 1 for the next One UI release, and so on. Point releases such as
     * One UI 8.5 get their own letter, so this is not the same as the Android upgrade count. Null for betas.
     */
    val majorUpgradeIndex: Int? get() = if (isBeta) null else oneUiIteration - 'A'

    val buildTag: String get() = raw.takeLast(SUFFIX_LENGTH)

    /** Sort key that orders builds the way Samsung releases them. Beta builds sort by date only. */
    val sortKey: Long
        get() = listOf(
            bootloader.toLong(),
            (majorUpgradeIndex ?: 0).toLong(),
            year.toLong(),
            month.toLong(),
            buildIteration.toLong(),
        ).fold(0L) { acc, part -> acc * SORT_RADIX + part }

    fun isNewerThan(other: FirmwareBuild): Boolean = sortKey > other.sortKey

    companion object {
        const val BETA_MARKER = 'Z'
        const val SUFFIX_LENGTH = 6
        private const val REGION_LENGTH = 2
        private const val MIN_LENGTH = SUFFIX_LENGTH + REGION_LENGTH + 3
        private const val SORT_RADIX = 100L
    }
}

enum class UpdateType(val code: Char, val label: String) {
    FEATURE('U', "Feature and security update"),
    SECURITY('S', "Security update only"),
    OTHER('?', "Other build type");

    companion object {
        fun from(code: Char): UpdateType = entries.firstOrNull { it.code == code } ?: OTHER
    }
}

object FirmwareDecoder {

    private val VALID = Regex("^[A-Z0-9]+$")
    private const val YEAR_BASE = 2001
    private const val YEAR_WRAP = 26
    private const val FIRST_MODERN_YEAR = 2015

    /** Parses a PDA build (or the first part of a PDA/CSC/MODEM triple). Returns null if it doesn't look like one. */
    fun decode(input: String): FirmwareBuild? {
        val build = input.trim().uppercase().substringBefore('/')
        if (build.length < 11 || !VALID.matches(build)) return null

        val suffix = build.takeLast(FirmwareBuild.SUFFIX_LENGTH)
        val head = build.dropLast(FirmwareBuild.SUFFIX_LENGTH)

        val bootloader = alphaNumValue(suffix[1]) ?: return null
        val oneUi = suffix[2].takeIf { it.isLetter() } ?: return null
        val year = yearFromLetter(suffix[3]) ?: return null
        val month = monthFromLetter(suffix[4]) ?: return null
        val iteration = alphaNumValue(suffix[5]) ?: return null

        return FirmwareBuild(
            raw = build,
            modelCode = head.dropLast(2),
            region = head.takeLast(2),
            updateType = UpdateType.from(suffix[0]),
            bootloader = bootloader,
            oneUiIteration = oneUi,
            year = year,
            month = month,
            buildIteration = iteration,
        )
    }

    /** 1-9 then A=10, B=11 and so on. Samsung uses this for bootloader and build counters. */
    fun alphaNumValue(c: Char): Int? = when (c) {
        in '1'..'9' -> c - '0'
        in 'A'..'Z' -> c - 'A' + 10
        else -> null
    }

    fun alphaNumChar(value: Int): Char? = when (value) {
        in 1..9 -> '0' + value
        in 10..35 -> 'A' + (value - 10)
        else -> null
    }

    /** R=2018 ... X=2024, Y=2025, Z=2026, then the alphabet wraps (A=2027). */
    fun yearFromLetter(c: Char): Int? {
        if (c !in 'A'..'Z') return null
        val year = YEAR_BASE + (c - 'A')
        return if (year < FIRST_MODERN_YEAR) year + YEAR_WRAP else year
    }

    fun letterForYear(year: Int): Char? {
        val offset = (year - YEAR_BASE) % YEAR_WRAP
        return if (year < FIRST_MODERN_YEAR) null else 'A' + offset
    }

    /** A=January ... L=December. */
    fun monthFromLetter(c: Char): Int? = if (c in 'A'..'L') c - 'A' + 1 else null

    fun letterForMonth(month: Int): Char? = if (month in 1..12) 'A' + (month - 1) else null
}
