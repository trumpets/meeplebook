package app.meeplebook.core.preferences

import app.meeplebook.core.collection.model.CollectionViewMode

/**
 * Persistent user preferences for the app.
 * Defaults match the original in-session behaviour before preferences were added.
 */
data class UserPreferences(
    val startingScreen: StartingScreen = StartingScreen.OVERVIEW,
    val collectionViewMode: CollectionViewMode = CollectionViewMode.LIST,
    val collectionAlphabetJumpVisible: Boolean = true
)
