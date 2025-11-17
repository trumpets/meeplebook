package app.meeplebook.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.meeplebook.feature.login.LoginScreen
import app.meeplebook.feature.login.LoginViewModel

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                onLoginSuccess = { navController.navigate(Screen.Home.route) }
            )
        }
//        composable(Screen.Home.route) {
//            HomeScreen()
//        }
    }
}