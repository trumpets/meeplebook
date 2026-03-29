package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayUiState

class ValidationReducer {

    fun reduce(state: AddPlayUiState): AddPlayUiState {

        val canSave =
            state.gameId != null &&
                    state.gameName != null

        return state.copy(canSave = canSave)
    }
}