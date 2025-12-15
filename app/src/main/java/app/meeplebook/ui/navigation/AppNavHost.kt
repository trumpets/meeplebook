package app.meeplebook.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.meeplebook.feature.home.HomeScreen
import app.meeplebook.feature.login.LoginScreen

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
            HomeScreen(
                args.refreshOnLogin
            )
        }
    }
}