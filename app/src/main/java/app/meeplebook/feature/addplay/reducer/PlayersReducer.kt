package app.meeplebook.feature.addplay.reducer

import app.meeplebook.core.ui.architecture.Reducer
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.updateGameSelected
import javax.inject.Inject

/**
 * Orchestrates all player-related sub-reducers.
 *
 * Dispatches each event to the appropriate sub-reducer ([PlayerEditReducer],
 * [PlayerListReducer], [PlayerScoreReducer], or [PlayerColorReducer]) and
 * writes the resulting player list back into [AddPlayUiState.GameSelected.players].
 */
class PlayersReducer @Inject constructor(
    private val editReducer: PlayerEditReducer,
    private val listReducer: PlayerListReducer,
    private val scoreReducer: PlayerScoreReducer,
    private val colorReducer: PlayerColorReducer
) : Reducer<AddPlayUiState, AddPlayEvent> {

    override fun reduce(
        state: AddPlayUiState,
        event: AddPlayEvent
    ): AddPlayUiState {

        return state.updateGameSelected {
            val updatedPlayers = when (event) {
                is AddPlayEvent.PlayerEditEvent ->
                    editReducer.reduce(state = players.players, event)

                is AddPlayEvent.PlayerListEvent ->
                    listReducer.reduce(state = players.players, event)

                is AddPlayEvent.PlayerScoreEvent ->
                    scoreReducer.reduce(state = players.players, event)

                is AddPlayEvent.PlayerColorEvent ->
                    colorReducer.reduce(state = players.players, event)

                else -> players.players
            }

            copy(players = players.copy(players = updatedPlayers))
        }
    }
}