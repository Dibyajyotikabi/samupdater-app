package com.samupdater.app.data.site

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class LiveDeviceEnvelope(
    val success: Boolean,
    val data: LiveDeviceDto? = null,
)

@Serializable
internal data class LiveDeviceDto(
    val device: String? = null,
    val model: String,
    val csc: String,
    val build: String,
    val android: String? = null,
    @SerialName("one_ui") val oneUi: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("security_patch") val securityPatch: String? = null,
    val notes: String? = null,
    val history: List<ReleaseDto> = emptyList(),
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("fetched_at") val fetchedAt: String? = null,
)

@Serializable
internal data class ReleaseDto(
    val build: String,
    val android: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("security_patch") val securityPatch: String? = null,
    val notes: String? = null,
)

@Serializable
internal data class LiveDeviceError(val success: Boolean = false, val data: ErrorData? = null) {
    @Serializable
    data class ErrorData(val message: String? = null)
}

@Serializable
internal data class WpPostDto(
    val link: String,
    val date: String,
    val title: Rendered,
    val excerpt: Rendered,
) {
    @Serializable
    data class Rendered(val rendered: String)
}
