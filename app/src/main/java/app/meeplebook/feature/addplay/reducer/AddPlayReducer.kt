package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState

class AddPlayReducer(
    private val metaReducer: MetaReducer,
    private val playersReducer: PlayersReducer
) {

    fun reduce(
        state: AddPlayUiState,
        event: AddPlayEvent
    ): AddPlayUiState {

        val afterMeta = metaReducer.reduce(state, event)
        val afterPlayers = playersReducer.reduce(afterMeta, event)

        return afterPlayers;
    }
}