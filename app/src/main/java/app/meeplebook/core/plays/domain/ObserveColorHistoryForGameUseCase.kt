package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.plays.model.ColorHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for observing color history for a specific game.
 * Returns colors previously used for this game, sorted by usage frequency.
 */
class ObserveColorHistoryForGameUseCase @Inject constructor(
    private val playsRepository: PlaysRepository
) {
    /**
     * Observes color history for a specific game.
     *
     * @param gameId The BGG game ID.
     * @return Flow emitting list of [ColorHistory] sorted by use count (descending).
     */
    operator fun invoke(gameId: Long): Flow<List<ColorHistory>> = flow {
        val colorHistory = playsRepository.getColorHistoryForGame(gameId)
        emit(colorHistory)
    }
}
