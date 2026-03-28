package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState

class MetaReducer {

    fun reduce(
        state: AddPlayUiState,
        event: AddPlayEvent
    ): AddPlayUiState {

        return when (event) {
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