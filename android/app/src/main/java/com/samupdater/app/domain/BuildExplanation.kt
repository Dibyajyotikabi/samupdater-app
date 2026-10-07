package com.samupdater.app.domain

import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

data class BuildPart(val chars: String, val title: String, val meaning: String)

/** Turns a decoded build into plain-language rows for the decoder screen. */
object BuildExplanation {

    fun explain(build: FirmwareBuild): List<BuildPart> {
        val suffix = build.buildTag
        val monthName = Month.of(build.month).getDisplayName(TextStyle.FULL, Locale.US)
        return listOf(
            BuildPart(build.modelCode, "Model", "SM-${build.modelCode}"),
            BuildPart(build.region, "Region", regionMeaning(build.region)),
            BuildPart(suffix[0].toString(), "Update type", build.updateType.label),
            BuildPart(suffix[1].toString(), "Bootloader", "Binary ${build.bootloader}. You can't downgrade below this."),
            BuildPart(suffix[2].toString(), "One UI", oneUiMeaning(build)),
            BuildPart(suffix[3].toString(), "Year", build.year.toString()),
            BuildPart(suffix[4].toString(), "Month", monthName),
            BuildPart(suffix[5].toString(), "Build", "Revision ${build.buildIteration} of that month"),
        )
    }

    /**
     * The letter counts One UI releases for the model, point releases like One UI 8.5 included.
     * So it can run ahead of the Android upgrade count on the Device tab, and we say so.
     */
    private fun oneUiMeaning(build: FirmwareBuild): String = when (val index = build.majorUpgradeIndex) {
        null -> "One UI beta build"
        0 -> "The One UI version this model launched with"
        else -> "One UI release #${index + 1} for this model (A is the launch version). " +
            "Point releases like One UI 8.5 count too, so this can be higher than the number of Android upgrades."
    }

    private fun regionMeaning(region: String): String = when (region) {
        "XX" -> "Europe, India and other open markets"
        "ZC", "ZH" -> "China and Hong Kong"
        "KS" -> "South Korea"
        "SQ", "UE", "UQ" -> "United States"
        "DX", "DT" -> "Southeast Asia and Oceania"
        "UB" -> "Latin America"
        else -> "Regional build ($region)"
    }
}
