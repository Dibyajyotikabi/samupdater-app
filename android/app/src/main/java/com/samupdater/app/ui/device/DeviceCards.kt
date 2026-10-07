package com.samupdater.app.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.samupdater.app.ui.Loadable
import com.samupdater.app.ui.components.EmptyText
import com.samupdater.app.ui.components.InfoRow
import com.samupdater.app.ui.components.SectionCard
import com.samupdater.app.ui.components.openLink

@Composable
internal fun SetupPrompt(isSamsung: Boolean, onPick: () -> Unit) {
    SectionCard("Set up your device") {
        EmptyText(
            if (isSamsung) "I couldn't read your CSC automatically. Pick it once and I'll remember it."
            else "This doesn't look like a Galaxy phone. Enter the Samsung model you want to follow.",
        )
        AssistChip(onClick = onPick, label = { Text("Choose model and CSC") })
    }
}

@Composable
internal fun InstalledCard(state: DeviceUiState) {
    val profile = state.profile ?: return
    if (!state.isThisPhone) return
    SectionCard("On this phone") {
        InfoRow("Build", profile.pdaBuild, monospace = true)
        InfoRow("One UI", profile.oneUiVersion)
        InfoRow("Android", profile.androidVersion.toString())
        InfoRow("Security patch", profile.securityPatch)
        InfoRow("CSC", profile.csc ?: state.tracked?.csc)
    }
}

/** Plain details for the newest build. The hero card above handles status and notes. */
@Composable
internal fun LatestCard(state: DeviceUiState) {
    val info = (state.firmware as? Loadable.Ready)?.value ?: return
    SectionCard("Firmware details") {
        InfoRow("Build", info.latest.build, monospace = true)
        InfoRow("Android", info.latest.android)
        InfoRow("One UI", info.oneUi)
        InfoRow("Security patch", info.latest.securityPatch)
        InfoRow("Released", info.latest.releaseDate)
        InfoRow("Region", info.csc)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OutlookCard(state: DeviceUiState) {
    SectionCard("Major update outlook") {
        val eligibility = state.eligibility
        val device = state.catalogDevice
        if (eligibility == null || device == null) {
            EmptyText("Samsung's update promise for this model isn't in my list yet.")
            return@SectionCard
        }
        Text(
            "${eligibility.upgradesUsed} of ${eligibility.upgradesPromised} Android upgrades used",
            style = MaterialTheme.typography.bodyLarge,
        )
        LinearProgressIndicator(
            progress = { eligibility.upgradesUsed.toFloat() / eligibility.upgradesPromised.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            if (eligibility.isEligibleForNext) {
                "Eligible for Android ${eligibility.nextAndroid}" + (eligibility.nextOneUi?.let { " / One UI $it" } ?: "")
            } else {
                "No more major Android upgrades promised. Security updates may continue."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (device.securityTier.isNotBlank()) InfoRow("Security updates", device.securityTier)
        device.securityUntil?.let { InfoRow("Security support until", it) }
        UpcomingChips(state)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpcomingChips(state: DeviceUiState) {
    val next = state.eligibility?.nextOneUi ?: return
    val matches = state.upcoming.filter { it.oneUi.substringBefore('.') == next }
    if (matches.isEmpty()) return
    val context = LocalContext.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        matches.forEach { update ->
            SuggestionChip(
                onClick = { update.url?.let { openLink(context, it) } },
                label = { Text("One UI ${update.oneUi}: ${stageLabel(update.stage)}") },
            )
        }
    }
}

internal fun stageLabel(stage: String): String = when (stage.lowercase()) {
    "testing" -> "Testing spotted"
    "beta" -> "Beta live"
    "rolling_out" -> "Stable rolling out"
    "announced" -> "Announced"
    else -> stage.replace('_', ' ')
}

