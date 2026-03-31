package app.meeplebook.feature.addplay.reducer

import app.meeplebook.core.ui.uiTextEmpty
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.LocationState
import app.meeplebook.feature.addplay.PlayersState
import app.meeplebook.feature.addplay.updateGameSearch
import java.time.Instant
import javax.inject.Inject

/**
 * Reduces [AddPlayEvent.GameSearchEvent] events into
 * top-level game identity.
 * All other events are passed through unchanged.
 */
class GameSearchReducer @Inject constructor() {

    fun reduce(
        state: AddPlayUiState,
        event: AddPlayEvent
    ): AddPlayUiState {
        return state.updateGameSearch {
            when (event) {
                is AddPlayEvent.GameSearchEvent.GameSearchQueryChanged ->
                    copy(gameSearchQuery = event.query)

                is AddPlayEvent.GameSearchEvent.GameSelected ->
                    AddPlayUiState.GameSelected(
                        gameId = event.gameId,
                        gameName = event.gameName,
                        date = Instant.now(),
                        durationMinutes = null,
                        location = LocationState(
                            null,
                            emptyList(),
                            emptyList()
                        ),
                        players = PlayersState(
                            players = emptyList(),
                            colorsHistory = emptyList()
                        ),
                        playersByLocation = emptyList(),
                        quantity = 1,
                        incomplete = false,
                        comments = "",
                        isSaving = false,
                        error = uiTextEmpty()
                    )

                else -> this
            }
        }
    }
}
