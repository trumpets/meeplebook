package app.meeplebook.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.meeplebook.R
import app.meeplebook.feature.home.navigation.HomeNavHost
import app.meeplebook.feature.home.navigation.HomeTabScreen
import app.meeplebook.ui.theme.MeepleBookTheme

/**
 * Represents a navigation destination in the bottom navigation bar.
 */
enum class HomeNavigationDestination(
    val labelResId: Int,
    val icon: ImageVector,
    val contentDescriptionResId: Int,
    val route: HomeTabScreen
) {
    HOME(R.string.nav_home, Icons.Default.Home, R.string.nav_home, HomeTabScreen.Overview),
    COLLECTION(R.string.nav_collection, Icons.Default.CollectionsBookmark, R.string.nav_collection, HomeTabScreen.Collection),
    PLAYS(R.string.nav_plays, Icons.Default.BarChart, R.string.nav_plays, HomeTabScreen.Plays),
    PROFILE(R.string.nav_profile, Icons.Default.Person, R.string.nav_profile, HomeTabScreen.Profile)
}

/**
 * HomeScreen is a lightweight container that provides:
 * - Bottom Navigation Bar
 * - A child NavHost for tab navigation
 *
 * All feature-specific logic is delegated to individual feature screens.
 */
@Composable
fun HomeScreen(
    refreshOnLogin: Boolean
) {
    val tabNavController = rememberNavController()

    HomeScreenContent(
        refreshOnLogin,
        onNavItemClick = { destination ->
            tabNavController.navigate(destination.route) {
                // Pop up to start destination to avoid building up back stack
                popUpTo(tabNavController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        tabNavController = tabNavController
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    refreshOnLogin: Boolean = false,
    onNavItemClick: (HomeNavigationDestination) -> Unit = {},
    tabNavController: NavHostController
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry = tabNavController.currentBackStackEntryAsState().value
                val currentDestination = navBackStackEntry?.destination
                HomeNavigationDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(destination.route::class)
                        } == true,
                        onClick = { onNavItemClick(destination) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.contentDescriptionResId)
                            )
                        },
                        label = { Text(stringResource(destination.labelResId)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Delegate to HomeNavHost for tab routing
        HomeNavHost(
            refreshOnLogin = refreshOnLogin,
            navController = tabNavController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenPreview() {
    MeepleBookTheme {
        val previewNavController = rememberNavController()
        HomeScreenContent(refreshOnLogin = false, tabNavController = previewNavController)
    }
}
