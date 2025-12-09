package app.meeplebook.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.meeplebook.feature.home.HomeScreen
import app.meeplebook.feature.login.LoginScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Any = Login
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Home) {
                        // Clear login screen from back stack
                        popUpTo(Login) { inclusive = true }
                    }
                }
            )
        }
        composable<Home> {
            HomeScreen()
        }
    }
}