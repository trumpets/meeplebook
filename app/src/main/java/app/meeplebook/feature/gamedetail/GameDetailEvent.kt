package app.meeplebook.feature.gamedetail

import app.meeplebook.core.plays.model.PlayId

/**
 * User-initiated events for the Game Detail screen.
 */
sealed interface GameDetailEvent {

    /** User triggered a manual refresh (pull-to-refresh). */
    data object Refresh : GameDetailEvent

    /**
     * User tapped a play entry to view its details.
     *
     * @property playId The ID of the play that was tapped.
     */
    data class PlayClicked(val playId: PlayId) : GameDetailEvent

    /**
     * User tapped an external web link.
     *
     * @property link The link that was tapped.
     */
    data class WebLinkClicked(val link: GameWebLink) : GameDetailEvent
}
