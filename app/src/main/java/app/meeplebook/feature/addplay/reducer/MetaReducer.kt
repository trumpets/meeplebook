package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.updateGameSelected
import javax.inject.Inject

/**
 * Reduces [AddPlayEvent.MetadataEvent] events into
 * top-level play metadata: date, duration, location.
 * All other events are passed through unchanged.
 */
class MetaReducer @Inject constructor() {

    fun reduce(
        state: AddPlayUiState,
        event: AddPlayEvent
    ): AddPlayUiState {
        return state.updateGameSelected {
            when (event) {
                is AddPlayEvent.MetadataEvent.DateChanged -> copy(date = event.date)
                is AddPlayEvent.MetadataEvent.DurationChanged -> copy(durationMinutes = event.minutes)
                is AddPlayEvent.MetadataEvent.LocationChanged -> copy(location = location.copy(value = event.value))
                else -> this
            }
        }
    }
}