package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.PlayerEntryUi

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