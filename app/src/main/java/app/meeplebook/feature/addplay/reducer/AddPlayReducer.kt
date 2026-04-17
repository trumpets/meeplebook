package app.meeplebook.feature.addplay.reducer

import app.meeplebook.core.ui.architecture.Reducer
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState
import javax.inject.Inject

/**
 * Root reducer for the Add Play screen.
 *
 * Composes sub-reducers in a fixed pipeline, threading state through each in order:
 *
 * 1. [GameSearchReducer]          — handles game search events (query changes, game selection)
 * 2. [MetaReducer]                — handles play metadata events (date, duration, location)
 * 3. [PlayersReducer]             — handles all player-related events
 * 4. [AddEditPlayerDialogReducer] — handles all player-add/edit events
 *
 * Each sub-reducer receives the output of the previous one, so every event is
 * handled exactly once and validation always reflects the latest state.
 */
class AddPlayReducer @Inject constructor(
    private val gameSearchReducer: GameSearchReducer,
    private val metaReducer: MetaReducer,
    private val playersReducer: PlayersReducer,
    private val addEditPlayerDialogReducer: AddEditPlayerDialogReducer
) : Reducer<AddPlayUiState, AddPlayEvent> {

    /**
     * Runs [state] and [event] through the full sub-reducer pipeline.
     *
     * @param state The current UI state before the event.
     * @param event The user-driven event to process.
     * @return The new UI state after all sub-reducers (including validation) have run.
     */
    override fun reduce(
        state: AddPlayUiState,
        event: AddPlayEvent
    ): AddPlayUiState {

        val afterGameSearch = gameSearchReducer.reduce(state, event)
        val afterMeta = metaReducer.reduce(afterGameSearch, event)
        val afterPlayers = playersReducer.reduce(afterMeta, event)
        val afterAddEditPlayer = addEditPlayerDialogReducer.reduce(afterPlayers, event)

        return afterAddEditPlayer
    }
}