package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState
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

        return when (event) {
            is AddPlayEvent.GameSearchEvent.GameSearchQueryChanged ->
                state.copy(gameSearchQuery = event.query)

            is AddPlayEvent.GameSearchEvent.GameSelected ->
                state.copy(
                    gameId = event.gameId,
                    gameName = event.gameName,
                    gameSearchQuery = "",
                    gameSearchResults = emptyList()
                )

            is AddPlayEvent.MetadataEvent.DateChanged ->
                state.copy(date = event.date)

            is AddPlayEvent.MetadataEvent.DurationChanged ->
                state.copy(durationMinutes = event.minutes)

            is AddPlayEvent.MetadataEvent.LocationChanged ->
                state.copy(location = state.location.copy(value = event.value))

            is AddPlayEvent.MetadataEvent.LocationSuggestionSelected ->
                state.copy(location = state.location.copy(value = event.value))

            else ->
                state
        }
    }
}