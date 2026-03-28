package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.PlayerEntryUi

class PlayerListReducer {

    fun reduce(
        players: List<PlayerEntryUi>,
        event: AddPlayEvent.PlayerListEvent
    ): List<PlayerEntryUi> {

        return when (event) {

            is AddPlayEvent.PlayerListEvent.AddEmptyPlayer ->
                players + PlayerEntryUi.empty(event.startPosition)

            is AddPlayEvent.PlayerListEvent.AddPlayerFromSuggestion ->
                players + PlayerEntryUi.fromPlayerIdentity(event.playerId, event.startPosition)

            is AddPlayEvent.PlayerListEvent.RemovePlayer ->
                players.filterNot { it.playerIdentity == event.playerEntryId }

            else ->
                players
        }
    }
}