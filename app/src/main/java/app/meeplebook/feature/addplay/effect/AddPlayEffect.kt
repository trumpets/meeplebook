package app.meeplebook.feature.addplay.effect

import app.meeplebook.core.plays.domain.CreatePlayCommand

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
     * Request a fresh load of player suggestions for the given [location].
     *
     * Produced whenever the location text changes so that the suggestions list stays
     * in sync with what the user has typed.
     *
     * @property location The current location text entered by the user.
     */
    data class LoadPlayerSuggestions(
        val location: String
    ) : AddPlayEffect

    /**
     * Persist [play] to local storage and schedule a sync with BGG.
     *
     * Produced when the user taps Save and [app.meeplebook.feature.addplay.AddPlayUiState.canSave] is `true`.
     *
     * @property play The fully-constructed command to be passed to the repository.
     */
    data class SavePlay(
        val play: CreatePlayCommand
    ) : AddPlayEffect
}
