package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.PlayerEntryUi

class PlayerEditReducer {

    fun reduce(
        players: List<PlayerEntryUi>,
        event: AddPlayEvent.PlayerEditEvent
    ): List<PlayerEntryUi> {

        return when (event) {

            is AddPlayEvent.PlayerEditEvent.NameChanged ->
                players.map {
                    if (it.playerIdentity == event.playerEntryId)
                        it.copy(playerIdentity = it.playerIdentity.copy (name = event.name))
                    else it
                }

            is AddPlayEvent.PlayerEditEvent.UsernameChanged ->
                players.map {
                    if (it.playerIdentity == event.playerEntryId)
                        it.copy(playerIdentity = it.playerIdentity.copy (username = event.username))
                    else it
                }

            is AddPlayEvent.PlayerEditEvent.TeamChanged ->
                players.map {
                    if (it.playerIdentity == event.playerEntryId)
                        it.copy(color = event.team)
                    else it
                }
        }
    }
}