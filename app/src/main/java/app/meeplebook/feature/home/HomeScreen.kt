package app.meeplebook.feature.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.meeplebook.R
import app.meeplebook.core.ui.scaffold.DefaultScaffoldController
import app.meeplebook.core.ui.scaffold.LocalScaffoldController
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
    val scaffoldController = rememberScaffoldController()

    CompositionLocalProvider(LocalScaffoldController provides scaffoldController) {
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
}

/**
 * Testable navigation bar component for HomeScreen.
 * Extracted as a separate composable to allow testing without NavHost dependencies.
 *
 * @param currentDestination The currently selected destination
 * @param onNavItemClick Callback when a navigation item is clicked
 * @param modifier Modifier to be applied to the NavigationBar
 */
@Composable
fun HomeNavigationBar(
    currentDestination: HomeNavigationDestination?,
    modifier: Modifier = Modifier,
    onNavItemClick: (HomeNavigationDestination) -> Unit = {},
) {
    NavigationBar(modifier = modifier) {
        HomeNavigationDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentDestination == destination,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    refreshOnLogin: Boolean = false,
    onNavItemClick: (HomeNavigationDestination) -> Unit = {},
    tabNavController: NavHostController
) {
    val scaffoldController = LocalScaffoldController.current
    val fabState by scaffoldController.fabState.collectAsState()

    val currentBackStackEntry by tabNavController.currentBackStackEntryAsState()

    // Track previous destination to detect *changes* (not initial load)
    val previousDestination = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentBackStackEntry) {
        val currentRoute = currentBackStackEntry?.destination?.route

        // Only clear if this is a navigation *change*, not initial load
        if (previousDestination.value != null && previousDestination.value != currentRoute) {
            scaffoldController.clearFab()
        }

        previousDestination.value = currentRoute
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = scaffoldController.snackbarHostState
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabState != null,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                val state = fabState ?: return@AnimatedVisibility

                FloatingActionButton(
                    onClick = { state.onClick.invoke() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag(state.testTag ?: "")
                ) {
                    Icon(
                        imageVector = state.icon,
                        contentDescription = state.contentDescription
                    )
                }
            }
        },
        bottomBar = {
            val navBackStackEntry = tabNavController.currentBackStackEntryAsState().value
            val currentDestination = navBackStackEntry?.destination
            val selectedDestination = HomeNavigationDestination.entries.firstOrNull { destination ->
                currentDestination?.hierarchy?.any {
                    it.hasRoute(destination.route::class)
                } == true
            }
            
            HomeNavigationBar(
                currentDestination = selectedDestination,
                onNavItemClick = onNavItemClick
            )
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

@Composable
fun rememberScaffoldController(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
): DefaultScaffoldController {
    return remember(snackbarHostState) {
        DefaultScaffoldController(snackbarHostState)
    }
}


@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeNavigationBarPreview() {
    MeepleBookTheme {
        HomeNavigationBar(
            currentDestination = HomeNavigationDestination.HOME
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
