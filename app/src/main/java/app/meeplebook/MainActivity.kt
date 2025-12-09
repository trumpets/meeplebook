package app.meeplebook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.ui.navigation.AppNavHost
import app.meeplebook.ui.navigation.Home
import app.meeplebook.ui.navigation.Login
import app.meeplebook.ui.theme.MeepleBookTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeepleBookTheme {
                Surface {
                    val navController = rememberNavController()
                    var initialRoute by remember { mutableStateOf<Any?>(null) }
                    
                    // Check for existing credentials at startup
                    LaunchedEffect(Unit) {
                        val user = authRepository.getCurrentUser()
                        initialRoute = if (user != null) Home else Login
                    }
                    
                    // Only show nav host once we've determined the initial route
                    initialRoute?.let { route ->
                        AppNavHost(
                            navController = navController,
                            startDestination = route
                        )
                    }
                }
            }
        }
    }
}
