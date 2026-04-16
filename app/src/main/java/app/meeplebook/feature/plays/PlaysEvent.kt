package app.meeplebook.feature.plays

import app.meeplebook.core.plays.model.PlayId

/**
 * Represents user actions and system events for the Plays screen.
 *
 * These events are emitted from the UI layer and processed by the ViewModel
 * to update the plays state, including search, data refresh, and navigation.
 */
sealed interface PlaysEvent {

    data class SearchChanged(val query: String) : PlaysEvent

    sealed interface ActionEvent : PlaysEvent {
        data class PlayClicked(val playId: PlayId) : ActionEvent
        data object LogPlayClicked : ActionEvent
        data object Refresh : ActionEvent
    }
}
