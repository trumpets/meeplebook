package app.meeplebook.feature.addplay.reducer

import app.meeplebook.core.ui.architecture.Reducer
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.PlayerEntryUi
import javax.inject.Inject

/**
 * Reduces [AddPlayEvent.PlayerListEvent] events by adding or removing entries
 * from the player list.  Edit/navigation events (e.g. [AddPlayEvent.PlayerListEvent.EditPlayer])
 * are passed through unchanged.
 */
class PlayerListReducer @Inject constructor() : Reducer<List<PlayerEntryUi>, AddPlayEvent.PlayerListEvent> {

    override fun reduce(
        state: List<PlayerEntryUi>,
        event: AddPlayEvent.PlayerListEvent
    ): List<PlayerEntryUi> {

        return when (event) {

            is AddPlayEvent.PlayerListEvent.AddNewPlayer ->
                state + PlayerEntryUi.empty(
                    playerName = event.playerName,
                    startPosition = event.startPosition
                )

            is AddPlayEvent.PlayerListEvent.AddPlayerFromSuggestion ->
                state + PlayerEntryUi.fromPlayerIdentity(
                    playerIdentity = event.playerIdentity,
                    startPosition = event.startPosition
                )

            is AddPlayEvent.PlayerListEvent.RemovePlayer ->
                state.filterNot { it.playerIdentity == event.playerIdentity }
                    .renumbered()

            is AddPlayEvent.PlayerListEvent.PlayerReordered -> {
                val from = event.fromIndex.coerceIn(0, state.lastIndex)
                val to = event.toIndex.coerceIn(0, state.lastIndex)
                if (from == to) return state
                state.toMutableList()
                    .apply { add(to, removeAt(from)) }
                    .renumbered()
            }

            is AddPlayEvent.PlayerListEvent.RestorePlayer -> {
                val insertAt = event.atIndex.coerceIn(0, state.size)
                state.toMutableList()
                    .apply { add(insertAt, event.player) }
                    .renumbered()
            }

            else ->
                state
        }
    }
}

private fun List<PlayerEntryUi>.renumbered(): List<PlayerEntryUi> =
    mapIndexed { index, player -> player.copy(startPosition = index + 1) }