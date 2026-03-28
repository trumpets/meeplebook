package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState

/**
 * Root reducer for the Add Play screen.
 *
 * Composes [MetaReducer] and [PlayersReducer] in order, piping the state through
 * each sub-reducer so that every event is handled exactly once.
 */
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

        return afterPlayers
    }
}