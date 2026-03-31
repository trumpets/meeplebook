package app.meeplebook.ui.navigation

import app.meeplebook.feature.addplay.PreselectedGame
import kotlinx.serialization.Serializable

sealed interface Screen {

    @Serializable
    object Login : Screen

    @Serializable
    data class Home(val refreshOnLogin: Boolean = false) : Screen

    @Serializable
    data class AddPlay(
        val preselectedGame: PreselectedGame? = null
    ) : Screen
}