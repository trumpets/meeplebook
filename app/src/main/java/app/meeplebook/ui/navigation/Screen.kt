package app.meeplebook.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {

    @Serializable
    object Login : Screen

    @Serializable
    data class Home(val refreshOnLogin: Boolean = false) : Screen

    @Serializable
    object AddPlay : Screen

    @Serializable
    data class AddPlayForGame(
        val gameId: Long,
        val gameName: String
    ) : Screen
}