package app.meeplebook.feature.overview

import app.meeplebook.core.plays.model.PlayId

/**
 * User-driven events handled by the Overview reducer/effect pipeline.
 */
sealed interface OverviewEvent {
    /**
     * Actions originating from direct interaction with the Overview screen.
     */
    sealed interface ActionEvent : OverviewEvent {
        /**
         * Requests a sync refresh of the user's data.
         */
        data object Refresh : ActionEvent

        /**
         * Requests opening the add-play flow from the Overview FAB.
         */
        data object LogPlayClicked : ActionEvent

        /**
         * Requests navigation to the selected recent play.
         */
        data class RecentPlayClicked(val playId: PlayId) : ActionEvent

        /**
         * Requests navigation to the recently-added highlight's game.
         */
        data class RecentlyAddedClicked(val gameId: Long) : ActionEvent

        /**
         * Requests navigation to the suggested highlight's game.
         */
        data class SuggestedGameClicked(val gameId: Long) : ActionEvent
    }
}
