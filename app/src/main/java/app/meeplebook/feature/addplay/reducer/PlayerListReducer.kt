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
                    .renumbered()

            is AddPlayEvent.PlayerListEvent.PlayerReordered -> {
                val from = event.fromIndex.coerceIn(0, players.lastIndex)
                val to = event.toIndex.coerceIn(0, players.lastIndex)
                if (from == to) return players
                players.toMutableList()
                    .apply { add(to, removeAt(from)) }
                    .renumbered()
            }

            is AddPlayEvent.PlayerListEvent.RestorePlayer -> {
                val insertAt = event.atIndex.coerceIn(0, players.size)
                players.toMutableList()
                    .apply { add(insertAt, event.player) }
                    .renumbered()
            }

            else ->
                players
        }
    }
}

private fun List<PlayerEntryUi>.renumbered(): List<PlayerEntryUi> =
    mapIndexed { index, player -> player.copy(startPosition = index + 1) }