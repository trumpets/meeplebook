package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.PlayerEntryUi

class PlayerScoreReducer {

    fun reduce(
        players: List<PlayerEntryUi>,
        event: AddPlayEvent.PlayerScoreEvent
    ): List<PlayerEntryUi> {

        return when (event) {

            is AddPlayEvent.PlayerScoreEvent.ScoreChanged -> {
                val updatedPlayers = players.map {
                    if (it.playerIdentity == event.playerEntryId)
                        it.copy(score = event.score)
                    else it
                }

                val maxPoints = updatedPlayers
                    .mapNotNull { it.score }
                    .maxOrNull() ?: return updatedPlayers

                updatedPlayers.map {
                    it.copy(isWinner = it.score == maxPoints)
                }
            }

            is AddPlayEvent.PlayerScoreEvent.WinnerToggled ->
                players.map {
                    if (it.playerIdentity == event.playerEntryId)
                        it.copy(isWinner = event.isWinner)
                    else it
                }
        }
    }
}