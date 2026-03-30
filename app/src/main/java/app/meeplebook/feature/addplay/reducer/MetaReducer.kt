package app.meeplebook.feature.addplay.reducer

import app.meeplebook.core.ui.uiTextEmpty
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.LocationState
import app.meeplebook.feature.addplay.PlayersState
import app.meeplebook.feature.addplay.updateGameSelected
import java.time.Instant
import javax.inject.Inject

/**
 * Reduces [AddPlayEvent.MetadataEvent] and [AddPlayEvent.GameSearchEvent] events into
 * top-level play metadata: date, duration, location, and game identity.
 * All other events are passed through unchanged.
 */
class MetaReducer @Inject constructor() {

    fun reduce(
        state: AddPlayUiState,
        event: AddPlayEvent
    ): AddPlayUiState {
        return when (state) {

            is AddPlayUiState.GameSearch -> {
                when (event) {
                    is AddPlayEvent.GameSearchEvent.GameSearchQueryChanged ->
                        state.copy(gameSearchQuery = event.query)

                    is AddPlayEvent.GameSearchEvent.GameSelected ->
                        AddPlayUiState.GameSelected(
                            gameId = event.gameId,
                            gameName = event.gameName,
                            date = Instant.now(),
                            durationMinutes = null,
                            location = LocationState(
                                null,
                                emptyList(),
                                emptyList(),
                                isFocused = false
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
                            error = uiTextEmpty(),
                            canSave = false
                        )

                    else -> state
                }
            }

            is AddPlayUiState.GameSelected -> {
                state.updateGameSelected {
                    when (event) {
                        is AddPlayEvent.MetadataEvent.DateChanged -> copy(date = event.date)
                        is AddPlayEvent.MetadataEvent.DurationChanged -> copy(durationMinutes = event.minutes)
                        is AddPlayEvent.MetadataEvent.LocationChanged -> copy(location = location.copy(value = event.value))
                        is AddPlayEvent.MetadataEvent.LocationSuggestionSelected -> copy(location = location.copy(value = event.value))
                        else -> this
                    }
                }
            }
        }
    }
}