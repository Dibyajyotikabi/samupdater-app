package com.samupdater.app.ui.beta

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samupdater.app.domain.BetaProgram
import com.samupdater.app.domain.FirmwareBuild
import com.samupdater.app.domain.UpdateType
import com.samupdater.app.ui.appViewModel
import com.samupdater.app.ui.components.EmptyText
import com.samupdater.app.ui.components.InfoRow
import com.samupdater.app.ui.components.LoadableContent
import com.samupdater.app.ui.components.SectionCard
import com.samupdater.app.ui.components.openLink
import com.samupdater.app.ui.device.stageLabel
import com.samupdater.app.ui.theme.BuildNumberFont
import com.samupdater.app.ui.components.OneUiPage

@Composable
fun BetaScreen(vm: BetaViewModel = viewModel(factory = appViewModel { BetaViewModel(it.repository, it.settings) })) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    OneUiPage(
        title = "Beta and test builds",
        subtitle = "What Samsung is testing next.",
        actions = { IconButton(onClick = vm::refresh) { Icon(Icons.Outlined.Refresh, "Refresh") } },
    ) {
        item {
            SectionCard("One UI beta program") {
                LoadableContent(state.programs) { programs ->
                    if (programs.isEmpty()) EmptyText("No One UI beta is listed for ${state.tracked?.model.orEmpty()} right now.")
                    programs.forEach { BetaProgramItem(it) { url -> openLink(context, url) } }
                }
            }
        }
        item {
            SectionCard("Test builds on Samsung's server") {
                LoadableContent(state.tests, onRetry = vm::refresh) { summary -> TestBuilds(summary) }
            }
        }
        if (state.upcoming.isNotEmpty()) {
            item {
                SectionCard("Coming next") {
                    state.upcoming.forEach { update ->
                        ListItem(
                            headlineContent = { Text("One UI ${update.oneUi}" + (update.android?.let { " (Android $it)" } ?: "")) },
                            supportingContent = { Text(listOfNotNull(stageLabel(update.stage), update.note).joinToString(". ")) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BetaProgramItem(program: BetaProgram, onOpen: (String) -> Unit) {
    InfoRow("One UI ${program.oneUi}", if (program.isOpen) "Open" else program.status.replaceFirstChar { it.uppercase() })
    if (program.countries.isNotEmpty()) InfoRow("Countries", program.countries.joinToString(", "))
    program.latestBeta?.let { InfoRow("Latest beta", it, monospace = true) }
    program.updated?.let { InfoRow("Updated", it) }
    program.noticeUrl?.let { url -> AssistChip(onClick = { onOpen(url) }, label = { Text("Official notice") }) }
}

@Composable
private fun TestBuilds(summary: TestBuildSummary) {
    val result = summary.result
    if (result.totalHashes == 0) {
        EmptyText("Samsung isn't listing any test builds for this model and CSC.")
        return
    }
    val inTesting = result.inTesting
    Text(
        when {
            summary.nextMajorInTesting -> "The next major update is being tested for this device."
            inTesting.isNotEmpty() -> "Samsung is testing new builds for this device."
            else -> "No decoded test build is newer than the current stable right now."
        },
        style = MaterialTheme.typography.bodyLarge,
    )
    InfoRow("Test builds listed", result.totalHashes.toString())
    InfoRow("Decoded", "${result.totalHashes - result.undecodedCount} of ${result.totalHashes}")
    InfoRow("Newer than your stable", inTesting.size.toString())
    val shown = inTesting.ifEmpty { result.decoded }.take(MAX_SHOWN)
    if (inTesting.isEmpty() && shown.isNotEmpty()) EmptyText("Most recent decoded builds, already released:")
    shown.forEachIndexed { index, build ->
        if (index > 0) HorizontalDivider()
        TestBuildItem(build)
    }
    if (result.undecodedCount > 0) {
        EmptyText(
            "${result.undecodedCount} listed builds use a format the decoder doesn't cover yet. " +
                "The list also keeps older test builds, so this number doesn't mean they are all new.",
        )
    }
}

@Composable
private fun TestBuildItem(build: FirmwareBuild) {
    val label = when {
        build.isBeta -> "Beta"
        build.updateType == UpdateType.FEATURE -> "Feature update"
        else -> "Security update"
    }
    ListItem(
        headlineContent = { Text(build.raw, fontFamily = BuildNumberFont) },
        supportingContent = { Text("$label, ${build.year}-${"%02d".format(build.month)}") },
    )
}

private const val MAX_SHOWN = 10
