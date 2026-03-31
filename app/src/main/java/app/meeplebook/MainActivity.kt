package app.meeplebook

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.ui.navigation.AppNavHost
import app.meeplebook.ui.navigation.Screen
import app.meeplebook.ui.theme.MeepleBookTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    /**
     * Emits a navigation target whenever [onNewIntent] is called while the activity
     * is already running (singleTop re-use). The composable collects this to navigate
     * without restarting the activity.
     */
    private val widgetNavigationEvents = MutableSharedFlow<Screen>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeepleBookTheme {
                Surface {
                    val navController = rememberNavController()
                    var initialRoute by remember { mutableStateOf<Screen?>(null) }

                    // Forward widget navigation events that arrive while the app is running.
                    LaunchedEffect(navController) {
                        widgetNavigationEvents.collect { screen ->
                            navController.navigate(screen)
                        }
                    }

                    // Show splash screen while determining route
                    if (initialRoute == null) {
                        // Check for existing credentials at startup
                        LaunchedEffect(Unit) {
                            val user = authRepository.getCurrentUser()
                            initialRoute = if (user != null) {
                                // Honour a widget tap that cold-started the app.
                                intentToScreen(intent) ?: Screen.Home(refreshOnLogin = false)
                            } else {
                                Screen.Login
                            }
                        }

                        SplashScreen()
                    } else {
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

    /**
     * Called when a widget tap arrives while the activity is already in the back-stack
     * ([android:launchMode="singleTop"] prevents a new instance from being created).
     * The intent is forwarded to the running composable via [widgetNavigationEvents].
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentToScreen(intent)?.let { widgetNavigationEvents.tryEmit(it) }
    }

    private fun intentToScreen(intent: Intent?): Screen? = when (intent?.action) {
        ACTION_ADD_PLAY -> Screen.AddPlay()
        // TODO: ACTION_VIEW_PLAY -> Screen.PlayDetails(playId) once that screen exists
        else -> null
    }

    companion object {
        /** Intent action sent by [app.meeplebook.widget.LogPlayAction]. */
        const val ACTION_ADD_PLAY = "app.meeplebook.ACTION_ADD_PLAY"

        /**
         * Intent action sent by [app.meeplebook.widget.OpenPlayAction].
         * Will navigate to the play-details screen once that feature is implemented.
         */
        const val ACTION_VIEW_PLAY = "app.meeplebook.ACTION_VIEW_PLAY"

        /** Long extra carrying the local play ID for [ACTION_VIEW_PLAY]. */
        const val EXTRA_PLAY_ID = "play_id"
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}