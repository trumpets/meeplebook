package app.meeplebook.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.meeplebook.feature.home.HomeNavigationDestination
import app.meeplebook.feature.home.HomeScreen
import app.meeplebook.feature.home.navigation.HomeTabScreen
import app.meeplebook.feature.login.LoginScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Any = Login
) {
    val homeTabNavController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable<Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Home(refreshOnLogin = true)) {
                        // Clear login screen from back stack
                        popUpTo(Login) { inclusive = true }
                    }
                }
            )
        }
        composable<Home> { backStackEntry ->
            val args = backStackEntry.toRoute<Home>()
            HomeScreen(
                args.refreshOnLogin,
                tabNavController = homeTabNavController
            )
        }
    }
}