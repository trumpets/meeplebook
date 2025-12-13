package app.meeplebook.feature.home.navigation

import kotlinx.serialization.Serializable

/**
 * Sealed class representing the different tab screens within Home.
 */
sealed interface HomeTabScreen {
    val routeString: String

    @Serializable
    data object Overview : HomeTabScreen {
        override val routeString = "home/overview"
    }

    @Serializable
    data object Collection : HomeTabScreen {
        override val routeString = "home/collection"
    }

    @Serializable
    data object Plays : HomeTabScreen {
        override val routeString = "home/plays"
    }

    @Serializable
    data object Profile : HomeTabScreen {
        override val routeString = "home/profile"
    }
}