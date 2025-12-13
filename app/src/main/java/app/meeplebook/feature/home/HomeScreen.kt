package app.meeplebook.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.meeplebook.R
import app.meeplebook.feature.home.navigation.HomeNavHost
import app.meeplebook.feature.home.navigation.HomeTabScreen

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
 * - Top App Bar
 * - Floating Action Button
 * - Bottom Navigation Bar
 * - A child NavHost for tab navigation
 *
 * All feature-specific logic is delegated to individual feature screens.
 */
@Composable
fun HomeScreen() {
    val tabNavController = rememberNavController()
    
    // Track selected tab using NavController state (not ViewModel state)
    val currentBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    
    // Match route to determine selected tab
    val selectedTab = HomeNavigationDestination.entries.find { destination ->
        currentRoute?.contains(destination.route::class.simpleName ?: "") == true
    } ?: HomeNavigationDestination.HOME

    HomeScreenContent(
        selectedNavItem = selectedTab,
        onNavItemClick = { destination ->
            tabNavController.navigate(destination.route) {
                // Pop up to start destination to avoid building up back stack
                popUpTo(HomeTabScreen.Overview) {
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
    selectedNavItem: HomeNavigationDestination = HomeNavigationDestination.HOME,
    onNavItemClick: (HomeNavigationDestination) -> Unit = {},
    onLogPlayClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    tabNavController: androidx.navigation.NavHostController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier.testTag("profileButton")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.profile_action_description)
                        )
                    }
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier.testTag("moreButton")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_options_description)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onLogPlayClick,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("logPlayFab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.log_play_description)
                )
            }
        },
        bottomBar = {
            NavigationBar {
                HomeNavigationDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = selectedNavItem == destination,
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
            navController = tabNavController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
