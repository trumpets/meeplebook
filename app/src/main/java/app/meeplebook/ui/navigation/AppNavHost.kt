package app.meeplebook.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import android.util.Log
import app.meeplebook.core.preferences.StartingScreen
import app.meeplebook.feature.home.HomeScreen
import app.meeplebook.feature.login.LoginScreen

private const val TAG = "AppNavHost"

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Any = Screen.Login
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Screen.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home(refreshOnLogin = true)) {
                        // Clear login screen from back stack
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                }
            )
        }
        composable<Screen.Home> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.Home>()
            val startingTab = try {
                StartingScreen.valueOf(args.startingScreen)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Unknown startingScreen value '${args.startingScreen}', defaulting to OVERVIEW")
                StartingScreen.OVERVIEW
            }
            HomeScreen(
                refreshOnLogin = args.refreshOnLogin,
                startingTab = startingTab,
                onLogout = {
                    navController.navigate(Screen.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
