package com.samupdater.app.ui.device

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.samupdater.app.domain.FirmwareInfo
import com.samupdater.app.domain.UpdateState
import com.samupdater.app.domain.VersionNumber
import com.samupdater.app.ui.components.ChevronLink
import com.samupdater.app.ui.components.OneUiCard
import com.samupdater.app.ui.components.PillButton
import com.samupdater.app.ui.components.openLink
import com.samupdater.app.ui.theme.BuildNumberFont

/** The main card: version banner, update status, the newest build and its notes. Info only, nothing to install. */
@Composable
internal fun HeroCard(state: DeviceUiState, info: FirmwareInfo) {
    val context = LocalContext.current
    var showNotes by rememberSaveable(info.latest.build) { mutableStateOf(false) }
    val hasNotes = state.notes.isNotEmpty() || !info.latest.notes.isNullOrBlank()
    OneUiCard {
        Column {
            HeroBannerFor(info)
            Column(
                Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusLine(state.updateState)
                Text(headline(state.updateState), style = MaterialTheme.typography.headlineSmall)
                Text(info.latest.build, style = MaterialTheme.typography.titleMedium, fontFamily = BuildNumberFont)
                Grey(versionDetails(info))
                Grey(listOfNotNull(info.latest.releaseDate?.let { "Released $it" }, info.model, info.csc))
                if (hasNotes) {
                    PillButton(
                        text = if (showNotes) "Hide what's new" else "See what's new",
                        icon = Icons.Outlined.AutoAwesome,
                        onClick = { showNotes = !showNotes },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                AnimatedVisibility(showNotes) { NotesContent(state, info) }
                info.sourceUrl?.let { url ->
                    ChevronLink("Samsung release notes", onClick = { openLink(context, url) }, Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

private fun versionDetails(info: FirmwareInfo): List<String> = listOfNotNull(
    VersionNumber.from(info.latest.android)?.let { "Android $it" },
    info.latest.securityPatch?.let { "Security patch $it" },
)

@Composable
private fun HeroBannerFor(info: FirmwareInfo) {
    val oneUi = VersionNumber.from(info.oneUi)
    val caption = listOfNotNull(info.deviceName, "latest stable").joinToString(" · ")
    if (oneUi != null) {
        HeroBanner("One UI", oneUi, caption, Modifier.padding(8.dp))
    } else {
        HeroBanner("Android", VersionNumber.from(info.latest.android) ?: "?", caption, Modifier.padding(8.dp))
    }
}

@Composable
private fun NotesContent(state: DeviceUiState, info: FirmwareInfo) {
    Column(Modifier.padding(top = 4.dp)) {
        if (state.notes.isNotEmpty()) {
            ReleaseNotesView(state.notes)
        } else {
            Text(info.latest.notes.orEmpty(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatusLine(updateState: UpdateState) {
    val color = if (updateState == UpdateState.UP_TO_DATE) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(STATUS_DOT.dp).background(color, CircleShape))
        Text(statusLabel(updateState), style = MaterialTheme.typography.labelLarge, color = color, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Grey(parts: List<String>) {
    if (parts.isEmpty()) return
    Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun statusLabel(state: UpdateState): String = when (state) {
    UpdateState.UP_TO_DATE -> "Up to date"
    UpdateState.UPDATE_AVAILABLE -> "Update available"
    UpdateState.ON_BETA -> "Beta build installed"
    UpdateState.UNKNOWN -> "Latest stable"
}

private fun headline(state: UpdateState): String = when (state) {
    UpdateState.UP_TO_DATE -> "You're on the newest build."
    UpdateState.UPDATE_AVAILABLE -> "A new update is out for you."
    UpdateState.ON_BETA -> "You're testing what's next."
    UpdateState.UNKNOWN -> "Here's the newest stable build."
}

private const val STATUS_DOT = 8
