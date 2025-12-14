package app.meeplebook.feature.home.navigation

import kotlinx.serialization.Serializable

/**
 * Sealed class representing the different tab screens within Home.
 */
sealed interface HomeTabScreen {

    @Serializable
    data object Overview : HomeTabScreen

    @Serializable
    data object Collection : HomeTabScreen

    @Serializable
    data object Plays : HomeTabScreen

    @Serializable
    data object Profile : HomeTabScreen
}