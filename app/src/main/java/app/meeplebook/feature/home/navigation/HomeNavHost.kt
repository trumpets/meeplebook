package app.meeplebook.feature.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.meeplebook.core.preferences.StartingScreen
import app.meeplebook.feature.collection.CollectionScreen
import app.meeplebook.feature.overview.OverviewScreen
import app.meeplebook.feature.plays.PlaysScreen
import app.meeplebook.feature.profile.ProfileScreen

/**
 * Navigation host for managing tabs within the Home screen.
 * Routes between Overview, Collection, Plays, and Profile screens.
 *
 * @param refreshOnLogin Whether to refresh data on login
 * @param startingTab Which tab to show first; defaults to Overview
 * @param onLogout Called when the user logs out from the Profile screen
 * @param navController The NavController for tab navigation
 * @param modifier Modifier to be applied to the NavHost
 */
@Composable
fun HomeNavHost(
    refreshOnLogin: Boolean,
    startingTab: StartingScreen,
    onLogout: () -> Unit,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val startDestination: HomeTabScreen = when (startingTab) {
        StartingScreen.OVERVIEW -> HomeTabScreen.Overview
        StartingScreen.COLLECTION -> HomeTabScreen.Collection
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<HomeTabScreen.Overview> {
            OverviewScreen(
                refreshOnLogin = refreshOnLogin
            )
        }
        composable<HomeTabScreen.Collection> {
            CollectionScreen()
        }
        composable<HomeTabScreen.Plays> {
            PlaysScreen()
        }
        composable<HomeTabScreen.Profile> {
            ProfileScreen(onLogout = onLogout)
        }
    }
}
