package app.meeplebook.feature.addplay.reducer

import app.meeplebook.core.ui.architecture.Reducer
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.PlayerEntryUi
import javax.inject.Inject

/**
 * Reduces [AddPlayEvent.PlayerColorEvent] events by updating the colour assigned
 * to the matching player entry.  Colour-picker open events are UI-only and are
 * passed through unchanged.
 */
class PlayerColorReducer @Inject constructor() : Reducer<List<PlayerEntryUi>, AddPlayEvent.PlayerColorEvent> {

    override fun reduce(
        state: List<PlayerEntryUi>,
        event: AddPlayEvent.PlayerColorEvent
    ): List<PlayerEntryUi> {

        return when (event) {

            is AddPlayEvent.PlayerColorEvent.ColorSelected ->
                state.map {
                    if (it.playerIdentity == event.playerIdentity)
                        it.copy(color = event.color.colorString)
                    else it
                }

            else ->
                state
        }
    }
}