package app.meeplebook.feature.overview.effect

import app.meeplebook.core.plays.model.PlayId

/**
 * One-shot UI effects emitted by Overview.
 *
 * These are transient navigation-style outcomes that should not be stored in reducer state.
 */
sealed interface OverviewUiEffect {
    /**
     * Opens the add-play flow.
     */
    data object OpenAddPlay : OverviewUiEffect

    /**
     * Requests navigation to a specific play.
     */
    data class NavigateToPlay(val playId: PlayId) : OverviewUiEffect

    /**
     * Requests navigation to a specific game.
     */
    data class NavigateToGame(val gameId: Long) : OverviewUiEffect
}
