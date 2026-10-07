package com.samupdater.app.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.samupdater.app.BuildConfig
import com.samupdater.app.ui.components.openLink
import com.samupdater.app.ui.components.OneUiCard
import com.samupdater.app.ui.components.OneUiPage
import androidx.compose.ui.unit.dp

@Composable
fun MoreScreen(onDecoder: () -> Unit, onNews: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    OneUiPage(title = "More", subtitle = "Tools, news and settings.") {
        item {
            OneUiCard {
                Column(Modifier.padding(vertical = 8.dp)) {
                    MoreItem(Icons.Outlined.QrCodeScanner, "Firmware decoder", "See what every character in a build number means", onDecoder)
                    MoreItem(Icons.AutoMirrored.Outlined.Article, "News and guides", "Latest posts from samupdater.com", onNews)
                    MoreItem(Icons.Outlined.Settings, "Alerts and settings", "Choose which updates notify you", onSettings)
                }
            }
        }
        item {
            OneUiCard {
                Column(Modifier.padding(vertical = 8.dp)) {
                    MoreItem(Icons.Outlined.Public, "Open samupdater.com", "Trackers, checkers and update guides") {
                        openLink(context, BuildConfig.SITE_BASE_URL)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
    )
}
