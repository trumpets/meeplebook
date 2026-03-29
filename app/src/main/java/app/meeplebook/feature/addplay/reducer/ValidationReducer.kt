package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayUiState

/**
 * Derives the [AddPlayUiState.canSave] flag from the current state.
 *
 * Runs as the final step in the [AddPlayReducer] pipeline so that every event
 * automatically recomputes whether the form is ready to be saved.
 *
 * A play can be saved when **all** of the following hold:
 * - [AddPlayUiState.gameId] is non-null (a game has been selected), and
 * - [AddPlayUiState.gameName] is non-null.
 *
 * @see AddPlayReducer
 */
class ValidationReducer {

    /**
     * Computes [AddPlayUiState.canSave] and returns an updated copy of [state].
     *
     * All other fields are left unchanged.
     *
     * @param state The state to validate.
     * @return A copy of [state] with [AddPlayUiState.canSave] set to reflect current validity.
     */
    fun reduce(state: AddPlayUiState): AddPlayUiState {

        val canSave =
            state.gameId != null &&
                    state.gameName != null

        return state.copy(canSave = canSave)
    }
}