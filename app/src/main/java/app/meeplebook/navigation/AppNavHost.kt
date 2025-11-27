package app.meeplebook.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.meeplebook.feature.login.LoginScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Login) {
        composable<Login> {
            LoginScreen(
                onLoginSuccess = {
//                    navController.navigate(Home)
                    android.util.Log.d("Ivo" ,"Login successful!")
                }
            )
        }
//        composable<Home> {
//            HomeScreen()
//        }
    }
}