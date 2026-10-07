package com.samupdater.app.ui.device

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.samupdater.app.BuildConfig
import com.samupdater.app.ui.components.ChevronLink
import com.samupdater.app.ui.components.OneUiCard
import com.samupdater.app.ui.components.openLink

/** Footer card that sends readers to the samupdater.com homepage, where the trackers and guides are. */
@Composable
internal fun SiteLinkCard() {
    val context = LocalContext.current
    OneUiCard {
        Column(
            Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Trackers, checkers and update guides",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ChevronLink("Visit samupdater.com", onClick = { openLink(context, BuildConfig.SITE_BASE_URL) })
        }
    }
}
