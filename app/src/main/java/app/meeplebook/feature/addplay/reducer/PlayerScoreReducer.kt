package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.PlayerEntryUi
import javax.inject.Inject

/**
 * Reduces [AddPlayEvent.PlayerScoreEvent] events.
 *
 * On [AddPlayEvent.PlayerScoreEvent.ScoreChanged] the reducer also auto-marks the
 * player(s) with the highest score as winners.
 * On [AddPlayEvent.PlayerScoreEvent.WinnerToggled] the winner flag is set explicitly.
 */
class PlayerScoreReducer @Inject constructor() {

    fun reduce(
        players: List<PlayerEntryUi>,
        event: AddPlayEvent.PlayerScoreEvent
    ): List<PlayerEntryUi> {

        return when (event) {

            is AddPlayEvent.PlayerScoreEvent.ScoreChanged -> {
                val updatedPlayers = players.map {
                    if (it.playerIdentity == event.playerIdentity)
                        it.copy(score = event.score)
                    else it
                }

                // If no player has a score, leave winner flags unchanged.
                val maxPoints = updatedPlayers
                    .mapNotNull { it.score }
                    .maxOrNull() ?: return updatedPlayers

                updatedPlayers.map {
                    it.copy(isWinner = it.score != null && it.score == maxPoints)
                }
            }

            is AddPlayEvent.PlayerScoreEvent.WinnerToggled ->
                players.map {
                    if (it.playerIdentity == event.playerIdentity)
                        it.copy(isWinner = event.isWinner)
                    else it
                }
        }
    }
}