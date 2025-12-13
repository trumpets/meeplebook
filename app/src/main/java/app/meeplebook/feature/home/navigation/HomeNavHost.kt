package app.meeplebook.feature.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.meeplebook.feature.collection.CollectionScreen
import app.meeplebook.feature.overview.OverviewScreen
import app.meeplebook.feature.plays.PlaysScreen
import app.meeplebook.feature.profile.ProfileScreen

/**
 * Navigation host for managing tabs within the Home screen.
 * Routes between Overview, Collection, Plays, and Profile screens.
 *
 * @param navController The NavController for tab navigation
 * @param modifier Modifier to be applied to the NavHost
 */
@Composable
fun HomeNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeTabScreen.Overview.routeString,
        modifier = modifier
    ) {
        composable(HomeTabScreen.Overview.routeString) {
            OverviewScreen()
        }
        composable(HomeTabScreen.Collection.routeString) {
            CollectionScreen()
        }
        composable(HomeTabScreen.Plays.routeString) {
            PlaysScreen()
        }
        composable(HomeTabScreen.Profile.routeString) {
            ProfileScreen()
        }
    }
}