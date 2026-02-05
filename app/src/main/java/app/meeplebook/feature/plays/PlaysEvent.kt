package app.meeplebook.feature.plays

/**
 * Represents user actions and system events for the Plays screen.
 *
 * These events are emitted from the UI layer and processed by the ViewModel
 * to update the plays state, including search, data refresh, and navigation.
 */
sealed interface PlaysEvent {

    // Search
    data class SearchChanged(val query: String) : PlaysEvent

    // Actions
    data class PlayClicked(val playId: Long) : PlaysEvent
    data object LogPlayClicked : PlaysEvent


    // System
    data object Refresh : PlaysEvent
}