package com.samupdater.app.ui.updates

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samupdater.app.domain.FirmwareRelease
import com.samupdater.app.ui.appViewModel
import com.samupdater.app.ui.components.EmptyText
import com.samupdater.app.ui.components.LoadableContent
import com.samupdater.app.ui.components.SectionCard
import com.samupdater.app.ui.theme.BuildNumberFont
import com.samupdater.app.ui.components.OneUiPage

@Composable
fun UpdatesScreen(vm: UpdatesViewModel = viewModel(factory = appViewModel { UpdatesViewModel(it.repository, it.settings) })) {
    val state by vm.state.collectAsStateWithLifecycle()
    OneUiPage(
        title = "Updates",
        subtitle = "Rollout and release history.",
        actions = { IconButton(onClick = vm::refresh) { Icon(Icons.Outlined.Refresh, "Refresh") } },
    ) {
        item {
            SectionCard("Rollout by region") {
                LoadableContent(state.rollout, onRetry = vm::refresh) { rows ->
                    rows.forEach { RolloutItem(it) }
                }
            }
        }
        item {
            SectionCard("Release history for ${state.tracked?.csc.orEmpty()}") {
                LoadableContent(state.firmware, onRetry = vm::refresh) { info ->
                    val releases = listOf(info.latest) + info.history.filterNot { it.build == info.latest.build }
                    if (releases.isEmpty()) EmptyText("No history yet.")
                    releases.forEachIndexed { index, release ->
                        if (index > 0) HorizontalDivider()
                        ReleaseItem(release)
                    }
                }
            }
        }
    }
}

@Composable
private fun RolloutItem(row: RolloutRow) {
    ListItem(
        leadingContent = {
            Icon(
                if (row.hasNewest) Icons.Outlined.CheckCircle else Icons.Outlined.Schedule,
                contentDescription = if (row.hasNewest) "Has the newest build" else "Waiting",
                tint = if (row.hasNewest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        },
        headlineContent = { Text(listOfNotNull(row.csc, row.country).joinToString(" · ")) },
        supportingContent = { Text(row.build.orEmpty(), fontFamily = BuildNumberFont) },
        trailingContent = { row.date?.let { Text(it, style = MaterialTheme.typography.labelMedium) } },
    )
}

@Composable
private fun ReleaseItem(release: FirmwareRelease) {
    ListItem(
        headlineContent = { Text(release.build, fontFamily = BuildNumberFont) },
        supportingContent = {
            Text(listOfNotNull(release.android, release.securityPatch?.let { "Patch $it" }).joinToString(" · "))
        },
        trailingContent = { release.releaseDate?.let { Text(it, style = MaterialTheme.typography.labelMedium) } },
    )
}
