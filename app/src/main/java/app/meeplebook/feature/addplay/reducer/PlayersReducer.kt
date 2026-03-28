package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState

/**
 * Orchestrates all player-related sub-reducers.
 *
 * Dispatches each event to the appropriate sub-reducer ([PlayerEditReducer],
 * [PlayerListReducer], [PlayerScoreReducer], or [PlayerColorReducer]) and
 * writes the resulting player list back into [AddPlayUiState.players].
 */
class PlayersReducer(
    private val editReducer: PlayerEditReducer,
    private val listReducer: PlayerListReducer,
    private val scoreReducer: PlayerScoreReducer,
    private val colorReducer: PlayerColorReducer
) {

    fun reduce(
        state: AddPlayUiState,
        event: AddPlayEvent
    ): AddPlayUiState {

        val playersState = state.players

        val updatedPlayers = when(event) {
            is AddPlayEvent.PlayerEditEvent ->
                editReducer.reduce(playersState.players, event)

            is AddPlayEvent.PlayerListEvent ->
                listReducer.reduce(playersState.players, event)

            is AddPlayEvent.PlayerScoreEvent ->
                scoreReducer.reduce(playersState.players, event)

            is AddPlayEvent.PlayerColorEvent ->
                colorReducer.reduce(playersState.players, event)

            else -> playersState.players
        }

        return state.copy(players = playersState.copy(players = updatedPlayers))
    }
}