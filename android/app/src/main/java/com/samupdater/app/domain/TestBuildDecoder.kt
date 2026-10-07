package com.samupdater.app.domain

/**
 * Samsung lists internal test builds in version.test.xml as hashes (MD5, lately some SHA-256) of
 * "PDA/CSC/MODEM". We generate likely triples from the model's stable history until the hashes match.
 *
 * What the real lists taught us:
 * - The list holds old test builds too, back to before launch, so we search the whole history.
 * - The CSC part always uses M as its type letter (S938BOXM3ZYEA) and otherwise copies the PDA.
 * - The modem usually equals the PDA. Beta (Z) builds often keep the stable One UI letter in the modem
 *   (S938BXXU3ZYEA with modem S938BXXU3BYEA), and the modem can be a few revisions behind.
 */
class TestBuildDecoder {

    data class Result(
        val decoded: List<FirmwareBuild>,
        val totalHashes: Int,
        val matchedHashes: Int = decoded.size,
        val stable: FirmwareBuild? = null,
    ) {
        val undecodedCount: Int get() = totalHashes - matchedHashes
        val latest: FirmwareBuild? get() = decoded.maxByOrNull { it.sortKey }

        /** Builds ahead of the current stable, the ones Samsung is still testing. Older ones already shipped. */
        val inTesting: List<FirmwareBuild>
            get() = stable?.let { current -> decoded.filter { it.isAheadOf(current) } } ?: decoded

        val latestInTesting: FirmwareBuild? get() = inTesting.maxByOrNull { it.sortKey }
    }

    /**
     * @param stableTriple the latest stable "PDA/CSC/MODEM" for the model and CSC.
     * @param history older stable triples (or PDAs) from version.xml. Without them we only search near [stableTriple].
     */
    fun decode(stableTriple: String, hashes: Collection<String>, history: Collection<String> = emptyList()): Result {
        val targets = HashTargets(hashes)
        val parts = stableTriple.trim().uppercase().split('/')
        val stable = FirmwareDecoder.decode(parts.first())
        if (targets.isEmpty || stable == null || parts.size != TRIPLE_PARTS) {
            return Result(emptyList(), targets.size, stable = stable)
        }

        val space = TestBuildSearchSpace(history.mapNotNull(FirmwareDecoder::decode) + stable)
        val template = TripleTemplate(parts)
        val matched = linkedMapOf<String, FirmwareBuild>()
        for (triple in candidates(space, template)) {
            val hash = targets.match(triple) ?: continue
            FirmwareDecoder.decode(triple)?.let { matched.putIfAbsent(hash, it) }
            if (matched.size == targets.size) break
        }
        // Two hashes can share a PDA when only the modem differs, so list each build once.
        val builds = matched.values.distinctBy { it.raw }.sortedByDescending { it.sortKey }
        return Result(builds, targets.size, matched.size, stable)
    }

    private fun candidates(space: TestBuildSearchSpace, template: TripleTemplate): Sequence<String> = sequence {
        for (month in space.months) {
            val yearChar = FirmwareDecoder.letterForYear(month / MONTHS) ?: continue
            val monthChar = FirmwareDecoder.letterForMonth(month % MONTHS + 1) ?: continue
            val stableLetters = space.oneUiLetters(month)
            for (type in UPDATE_TYPES) {
                for (bootloader in space.bootloaders(month)) {
                    val blChar = FirmwareDecoder.alphaNumChar(bootloader) ?: continue
                    for (letter in stableLetters + FirmwareBuild.BETA_MARKER) {
                        val modemLetters = if (letter == FirmwareBuild.BETA_MARKER) listOf(letter) + stableLetters else listOf(letter)
                        val head = "$type$blChar$letter$yearChar$monthChar"
                        for (iteration in 1..MAX_ITERATION) {
                            val pda = head + alphaNum(iteration)
                            val modems = if (template.hasModem) modemSuffixes(type, blChar, modemLetters, yearChar, monthChar, iteration) else sequenceOf("")
                            for (modem in modems) {
                                yield(template.fill(pda, modem))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun modemSuffixes(
        type: Char,
        blChar: Char,
        letters: List<Char>,
        yearChar: Char,
        monthChar: Char,
        iteration: Int,
    ): Sequence<String> = sequence {
        for (letter in letters) {
            for (offset in MODEM_ITERATION_OFFSETS) {
                val modemIteration = FirmwareDecoder.alphaNumChar(iteration + offset) ?: continue
                yield("$type$blChar$letter$yearChar$monthChar$modemIteration")
            }
        }
    }

    private fun alphaNum(value: Int): Char = requireNotNull(FirmwareDecoder.alphaNumChar(value)) { "Out of range: $value" }

    /** The stable triple with the build suffixes cut off, ready to take generated ones. */
    private class TripleTemplate(parts: List<String>) {
        private val pdaPrefix = parts[0].dropLast(FirmwareBuild.SUFFIX_LENGTH)
        private val cscPrefix = parts[1].takeIf { it.length > FirmwareBuild.SUFFIX_LENGTH }?.dropLast(FirmwareBuild.SUFFIX_LENGTH - 1)
        private val modemPrefix = parts[2].takeIf { it.length > FirmwareBuild.SUFFIX_LENGTH }?.dropLast(FirmwareBuild.SUFFIX_LENGTH)
        val hasModem: Boolean get() = modemPrefix != null

        /** Wi-Fi only models have no modem, so that part stays empty like it does on Samsung's server. */
        fun fill(pdaSuffix: String, modemSuffix: String): String {
            val csc = cscPrefix?.let { it + pdaSuffix.drop(1) }.orEmpty()
            val modem = modemPrefix?.let { it + modemSuffix }.orEmpty()
            return "$pdaPrefix$pdaSuffix/$csc/$modem"
        }
    }

    private companion object {
        const val TRIPLE_PARTS = 3
        const val MONTHS = 12
        const val MAX_ITERATION = 35
        val UPDATE_TYPES = listOf('U', 'S')

        /** Same revision first, then the small gaps seen in real lists. */
        val MODEM_ITERATION_OFFSETS = listOf(0, -1, 1, -2, -3, -4)
    }
}

/** Betas sort by date only, so compare them to the stable by month instead of by sort key. */
private fun FirmwareBuild.isAheadOf(stable: FirmwareBuild): Boolean =
    if (isBeta) monthIndex >= stable.monthIndex else isNewerThan(stable)
