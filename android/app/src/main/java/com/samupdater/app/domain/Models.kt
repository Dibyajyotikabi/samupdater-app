package com.samupdater.app.domain

import kotlinx.serialization.Serializable

/** What the phone reports about itself. Fields are null when the system won't share them. */
data class DeviceProfile(
    val isSamsung: Boolean,
    val model: String?,
    val csc: String?,
    val pdaBuild: String?,
    val oneUiVersion: String?,
    val androidVersion: Int,
    val securityPatch: String?,
)

/** Model + CSC pair the app checks. Used for "my device" and every watchlist entry. */
@Serializable
data class TrackedDevice(val model: String, val csc: String, val nickname: String? = null) {
    val key: String get() = "$model|$csc"
    val label: String get() = nickname?.takeIf { it.isNotBlank() } ?: model
}

data class FirmwareRelease(
    val build: String,
    val android: String?,
    val releaseDate: String?,
    val securityPatch: String?,
    val notes: String?,
)

data class FirmwareInfo(
    val deviceName: String?,
    val model: String,
    val csc: String,
    val latest: FirmwareRelease,
    val oneUi: String?,
    val history: List<FirmwareRelease>,
    val sourceUrl: String?,
    val fetchedAt: String?,
)

enum class UpdateState { UP_TO_DATE, UPDATE_AVAILABLE, ON_BETA, UNKNOWN }

object UpdateStatus {
    fun compare(installedPda: String?, latestPda: String?): UpdateState {
        val installed = installedPda?.let(FirmwareDecoder::decode) ?: return UpdateState.UNKNOWN
        val latest = latestPda?.let(FirmwareDecoder::decode) ?: return UpdateState.UNKNOWN
        return when {
            installed.isBeta -> UpdateState.ON_BETA
            installed.raw == latest.raw -> UpdateState.UP_TO_DATE
            latest.isNewerThan(installed) -> UpdateState.UPDATE_AVAILABLE
            else -> UpdateState.UP_TO_DATE
        }
    }
}

@Serializable
data class Catalog(
    val updatedAt: String? = null,
    val devices: List<CatalogDevice> = emptyList(),
    val betaPrograms: List<BetaProgram> = emptyList(),
    val upcoming: List<UpcomingUpdate> = emptyList(),
    val cscSuggestions: List<CscOption> = emptyList(),
) {
    /** Longest model prefix wins, so SM-S938 beats SM-S93. */
    fun deviceFor(model: String): CatalogDevice? =
        devices.filter { model.startsWith(it.modelPrefix, ignoreCase = true) }.maxByOrNull { it.modelPrefix.length }

    fun betaFor(model: String): List<BetaProgram> =
        betaPrograms.filter { program -> program.modelPrefixes.any { model.startsWith(it, ignoreCase = true) } }
}

@Serializable
data class CatalogDevice(
    val modelPrefix: String,
    val name: String,
    val launchAndroid: Int,
    val osUpgrades: Int,
    val securityTier: String = "",
    val securityUntil: String? = null,
    val rolloutCscs: List<String> = emptyList(),
)

@Serializable
data class BetaProgram(
    val oneUi: String,
    val status: String,
    val modelPrefixes: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val latestBeta: String? = null,
    val noticeUrl: String? = null,
    val updated: String? = null,
) {
    val isOpen: Boolean get() = status.equals("open", ignoreCase = true)
}

@Serializable
data class UpcomingUpdate(
    val oneUi: String,
    val android: Int? = null,
    val stage: String,
    val note: String? = null,
    val url: String? = null,
)

@Serializable
data class CscOption(val code: String, val country: String)

data class Article(val title: String, val link: String, val date: String, val excerpt: String)
