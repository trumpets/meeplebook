package app.meeplebook.feature.plays

import app.meeplebook.core.plays.model.PlayId

/**
 * Events emitted by the Plays UI.
 *
 * These events feed the reducer/effect pipeline:
 * - synchronous state changes update [PlaysBaseState] through [app.meeplebook.feature.plays.reducer.PlaysReducer]
 * - one-shot work such as refresh or navigation is produced by
 *   [app.meeplebook.feature.plays.effect.PlaysEffectProducer]
 */
sealed interface PlaysEvent {
    /**
     * User changed the search query.
     *
     * This updates [PlaysBaseState.searchQuery] immediately; debounced domain querying derives
     * from that state later in the ViewModel.
     *
     * @property query The current query string from the search field.
     */
    data class SearchChanged(val query: String) : PlaysEvent

    /**
     * Action-style events that typically trigger effects rather than direct base-state mutation.
     */
    sealed interface ActionEvent : PlaysEvent {
        /** User tapped a play row and wants to open that play. */
        data class PlayClicked(val playId: PlayId) : ActionEvent

        /** User tapped the log-play affordance. Currently retained as a no-op placeholder event. */
        data object LogPlayClicked : ActionEvent

        /** User requested a manual refresh/sync of plays. */
        data object Refresh : ActionEvent
    }
}
