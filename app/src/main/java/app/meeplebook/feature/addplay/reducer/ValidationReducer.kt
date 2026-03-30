package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.updateGameSelected
import javax.inject.Inject

/**
 * Derives the [AddPlayUiState.canSave] flag from the current state.
 *
 * Runs as the final step in the [AddPlayReducer] pipeline so that every event
 * automatically recomputes whether the form is ready to be saved.
 *
 * A play can be saved when **all** of the following hold:
 * - [AddPlayUiState.gameName] is non-blank, and
 * - [AddPlayUiState.isSaving] is `false` (no save is already in progress).
 *
 * @see AddPlayReducer
 */
class ValidationReducer @Inject constructor() {

    /**
     * Computes [AddPlayUiState.canSave] and returns an updated copy of [state].
     *
     * All other fields are left unchanged.
     *
     * @param state The state to validate.
     * @return A copy of [state] with [AddPlayUiState.canSave] set to reflect current validity.
     */
    fun reduce(state: AddPlayUiState): AddPlayUiState {
        return state.updateGameSelected {
            val canSave =
                gameName.isNotBlank() && !isSaving

            copy(canSave = canSave)
        }
    }
}