package com.samupdater.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * One UI page: a slim icon row, a large two-line title with a grey subtitle, then cards on a grey page.
 * Every tab and sub-screen uses this so they all share the same header.
 */
@Composable
fun OneUiPage(
    title: String,
    subtitle: String? = null,
    logo: (@Composable () -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingAction: (@Composable () -> Unit)? = null,
    bottomSpace: Int = DEFAULT_BOTTOM_SPACE,
    content: LazyListScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            OneUiTopRow(onBack, actions)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = PAGE_GUTTER.dp, end = PAGE_GUTTER.dp, bottom = bottomSpace.dp),
                verticalArrangement = Arrangement.spacedBy(CARD_GAP.dp),
            ) {
                item { OneUiTitle(title, subtitle, logo) }
                content()
            }
        }
        if (floatingAction != null) {
            Box(Modifier.align(Alignment.BottomEnd).padding(PAGE_GUTTER.dp)) { floatingAction() }
        }
    }
}

@Composable
private fun OneUiTopRow(onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(TOP_ROW_HEIGHT.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        }
        Spacer(Modifier.weight(1f))
        actions()
    }
}

@Composable
private fun OneUiTitle(title: String, subtitle: String?, logo: (@Composable () -> Unit)?) {
    Row(
        Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (logo != null) {
            logo()
            Spacer(Modifier.width(14.dp))
        }
        TitleText(title, subtitle)
    }
}

@Composable
private fun TitleText(title: String, subtitle: String?) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private const val PAGE_GUTTER = 16
private const val CARD_GAP = 14
private const val TOP_ROW_HEIGHT = 56
private const val DEFAULT_BOTTOM_SPACE = 24
