package app.meeplebook.feature.addplay.reducer

import app.meeplebook.core.ui.architecture.Reducer
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.OptionalField
import app.meeplebook.feature.addplay.updateGameSelected
import javax.inject.Inject

/**
 * Reduces [AddPlayEvent.MetadataEvent] events into
 * top-level play metadata: date, duration, location.
 * All other events are passed through unchanged.
 */
class MetaReducer @Inject constructor() : Reducer<AddPlayUiState, AddPlayEvent> {

    override fun reduce(
        state: AddPlayUiState,
        event: AddPlayEvent
    ): AddPlayUiState {
        return state.updateGameSelected {
            when (event) {
                is AddPlayEvent.MetadataEvent.DateChanged -> copy(date = event.date)
                is AddPlayEvent.MetadataEvent.DurationChanged -> copy(durationMinutes = event.minutes)
                is AddPlayEvent.MetadataEvent.LocationChanged -> copy(location = location.copy(value = event.value))
                is AddPlayEvent.MetadataEvent.QuantityChanged -> copy(quantity = event.value ?: 1)
                is AddPlayEvent.MetadataEvent.IncompleteToggled -> copy(incomplete = event.value)
                is AddPlayEvent.MetadataEvent.CommentsChanged -> copy(comments = event.value)
                is AddPlayEvent.MetadataEvent.ShowOptionalField -> when (event.field) {
                    OptionalField.QUANTITY -> copy(showQuantity = true)
                    OptionalField.INCOMPLETE -> copy(showIncomplete = true, incomplete = true)
                    OptionalField.COMMENTS -> copy(showComments = true)
                }
                else -> this
            }
        }
    }
}