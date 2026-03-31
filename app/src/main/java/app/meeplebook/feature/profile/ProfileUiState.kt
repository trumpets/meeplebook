package app.meeplebook.feature.profile

import app.meeplebook.core.collection.model.CollectionViewMode
import app.meeplebook.core.preferences.StartingScreen

/**
 * UI state for the Profile / Settings screen.
 */
data class ProfileUiState(
    val username: String = "",
    val startingScreen: StartingScreen = StartingScreen.OVERVIEW,
    val collectionViewMode: CollectionViewMode = CollectionViewMode.LIST,
    val collectionAlphabetJumpVisible: Boolean = true,
    val isLoading: Boolean = false,
    val isLogoutConfirmVisible: Boolean = false
)
