package app.meeplebook.feature.profile

import app.meeplebook.core.collection.model.CollectionViewMode
import app.meeplebook.core.preferences.StartingScreen

/**
 * Represents user interactions on the Profile / Settings screen.
 */
sealed interface ProfileEvent {
    data class StartingScreenSelected(val screen: StartingScreen) : ProfileEvent
    data class CollectionViewModeSelected(val viewMode: CollectionViewMode) : ProfileEvent
    data class CollectionAlphabetJumpVisibilityChanged(val visible: Boolean) : ProfileEvent
    data object OpenSourceLicensesClicked : ProfileEvent
    data object LogoutClicked : ProfileEvent
    data object LogoutConfirmed : ProfileEvent
    data object LogoutDismissed : ProfileEvent
}
