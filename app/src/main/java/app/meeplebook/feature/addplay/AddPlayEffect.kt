package app.meeplebook.feature.addplay

import app.meeplebook.core.plays.model.Play

/**
 * Domain-level side effects produced by the Add Play feature.
 *
 * These are handled by the ViewModel and typically result in data/network operations
 * (e.g., fetching suggestions from the database or persisting a play).
 *
 * @see AddPlayUiEffect for effects that are handled by the Composable screen.
 */
sealed interface AddPlayEffect {

    /**
     * Request a fresh load of player suggestions for the given [gameId] and [location].
     *
     * This is produced whenever the location text changes or the user explicitly
     * requests a refresh, so that the suggestions list stays in sync.
     */
    data class LoadPlayerSuggestions(
        val gameId: Long,
        val location: String
    ) : AddPlayEffect

    /**
     * Persist [play] to local storage and schedule a sync with BGG.
     *
     * Produced when the user taps Save and the current state passes validation.
     */
    data class SavePlay(
        val play: Play
    ) : AddPlayEffect
}
