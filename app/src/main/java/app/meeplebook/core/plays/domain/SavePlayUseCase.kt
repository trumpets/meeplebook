package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.plays.model.NewPlay
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.Player
import app.meeplebook.core.result.AppResult
import javax.inject.Inject
import kotlin.random.Random

/**
 * Use case for saving a new play locally.
 *
 * NOTE: This currently only saves plays locally with a temporary negative ID.
 * Future implementation should POST to BGG API and get a real play ID.
 */
class SavePlayUseCase @Inject constructor(
    private val playsRepository: PlaysRepository
) {
    /**
     * Saves a new play locally.
     *
     * @param newPlay The new play to save.
     * @return Success with the saved play, or Failure with an error.
     */
    suspend operator fun invoke(newPlay: NewPlay): AppResult<Play, SavePlayError> {
        return try {
            // For now, generate a temporary negative ID for local-only plays
            // This will be replaced when we implement BGG API POST
            val tempId = -Random.nextLong(1, Long.MAX_VALUE)
            
            val play = Play(
                id = tempId,
                date = newPlay.date,
                quantity = newPlay.quantity,
                length = newPlay.length,
                incomplete = newPlay.incomplete,
                location = newPlay.location,
                gameId = newPlay.gameId,
                gameName = newPlay.gameName,
                comments = newPlay.comments,
                players = newPlay.players.mapIndexed { index, newPlayer ->
                    Player(
                        id = 0, // Room will auto-generate
                        playId = tempId,
                        username = newPlayer.username,
                        userId = newPlayer.userId,
                        name = newPlayer.name,
                        startPosition = newPlayer.startPosition,
                        color = newPlayer.color,
                        score = newPlayer.score,
                        win = newPlayer.win
                    )
                }
            )
            
            playsRepository.savePlay(play)
            AppResult.Success(play)
        } catch (e: Exception) {
            AppResult.Failure(SavePlayError.Unknown(e))
        }
    }
}

/**
 * Errors that can occur when saving a play.
 */
sealed class SavePlayError {
    data class Unknown(val exception: Exception) : SavePlayError()
    // Future: Add network errors, validation errors, etc.
}
