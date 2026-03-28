package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.PlayerEntryUi

/**
 * Reduces [AddPlayEvent.PlayerColorEvent] events by updating the colour assigned
 * to the matching player entry.  Colour-picker open events are UI-only and are
 * passed through unchanged.
 */
class PlayerColorReducer {

    fun reduce(
        players: List<PlayerEntryUi>,
        event: AddPlayEvent.PlayerColorEvent
    ): List<PlayerEntryUi> {

        return when (event) {

            is AddPlayEvent.PlayerColorEvent.ColorSelected ->
                players.map {
                    if (it.playerIdentity == event.playerEntryId)
                        it.copy(color = event.color.colorString)
                    else it
                }

            else ->
                players
        }
    }
}