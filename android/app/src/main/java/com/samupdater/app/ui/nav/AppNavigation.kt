package com.samupdater.app.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.samupdater.app.ui.beta.BetaScreen
import com.samupdater.app.ui.device.DeviceScreen
import com.samupdater.app.ui.more.DecoderScreen
import com.samupdater.app.ui.more.MoreScreen
import com.samupdater.app.ui.more.NewsScreen
import com.samupdater.app.ui.more.SettingsScreen
import com.samupdater.app.ui.updates.UpdatesScreen
import com.samupdater.app.ui.watchlist.WatchlistScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    DEVICE("device", "Device", Icons.Outlined.PhoneAndroid),
    UPDATES("updates", "Updates", Icons.Outlined.SystemUpdate),
    BETA("beta", "Beta", Icons.Outlined.Science),
    WATCHLIST("watchlist", "Watchlist", Icons.Outlined.Visibility),
    MORE("more", "More", Icons.Outlined.MoreHoriz),
}

private object MoreRoutes {
    const val HOME = "more/home"
    const val DECODER = "more/decoder"
    const val NEWS = "more/news"
    const val SETTINGS = "more/settings"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    Scaffold(bottomBar = { BottomBar(nav) }) { padding ->
        // Each screen draws its own top bar, so only the bottom inset is applied here.
        Box(Modifier.padding(bottom = padding.calculateBottomPadding()).consumeWindowInsets(padding)) {
            NavHost(nav, startDestination = Tab.DEVICE.route) {
                composable(Tab.DEVICE.route) { DeviceScreen() }
                composable(Tab.UPDATES.route) { UpdatesScreen() }
                composable(Tab.BETA.route) { BetaScreen() }
                composable(Tab.WATCHLIST.route) { WatchlistScreen() }
                navigation(startDestination = MoreRoutes.HOME, route = Tab.MORE.route) {
                    composable(MoreRoutes.HOME) {
                        MoreScreen(
                            onDecoder = { nav.navigate(MoreRoutes.DECODER) },
                            onNews = { nav.navigate(MoreRoutes.NEWS) },
                            onSettings = { nav.navigate(MoreRoutes.SETTINGS) },
                        )
                    }
                    composable(MoreRoutes.DECODER) { DecoderScreen(onBack = nav::popBackStack) }
                    composable(MoreRoutes.NEWS) { NewsScreen(onBack = nav::popBackStack) }
                    composable(MoreRoutes.SETTINGS) { SettingsScreen(onBack = nav::popBackStack) }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController) {
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    // One UI style: flat bar, no pill indicator, the selected tab just turns navy and bold.
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.secondary,
        selectedTextColor = MaterialTheme.colorScheme.secondary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = Color.Transparent,
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Tab.entries.forEach { tab ->
            val selected = destination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                colors = itemColors,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, null) },
                label = { Text(tab.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
            )
        }
    }
}
