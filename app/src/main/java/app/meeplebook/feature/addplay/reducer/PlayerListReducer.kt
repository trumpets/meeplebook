package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.PlayerEntryUi
import javax.inject.Inject

/**
 * Reduces [AddPlayEvent.PlayerListEvent] events by adding or removing entries
 * from the player list.  Edit/navigation events (e.g. [AddPlayEvent.PlayerListEvent.EditPlayer])
 * are passed through unchanged.
 */
class PlayerListReducer @Inject constructor() {

    fun reduce(
        players: List<PlayerEntryUi>,
        event: AddPlayEvent.PlayerListEvent
    ): List<PlayerEntryUi> {

        return when (event) {

            is AddPlayEvent.PlayerListEvent.AddNewPlayer ->
                players + PlayerEntryUi.empty(
                    playerName = event.playerName,
                    startPosition = event.startPosition
                )

            is AddPlayEvent.PlayerListEvent.AddPlayerFromSuggestion ->
                players + PlayerEntryUi.fromPlayerIdentity(
                    playerIdentity = event.playerIdentity,
                    startPosition = event.startPosition
                )

            is AddPlayEvent.PlayerListEvent.RemovePlayer ->
                players.filterNot { it.playerIdentity == event.playerIdentity }

            else ->
                players
        }
    }
}