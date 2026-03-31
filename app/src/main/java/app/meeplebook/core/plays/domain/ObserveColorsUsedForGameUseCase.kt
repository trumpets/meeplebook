package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.plays.model.PlayerColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Observes distinct player colors used for a specific game.
 *
 * Returns [PlayerColor] entries that have been recorded for plays of [gameId],
 * sorted by enum ordinal. Colors that do not map to a known [PlayerColor] value
 * are silently dropped.
 */
class ObserveColorsUsedForGameUseCase @Inject constructor(
    private val playsRepository: PlaysRepository
) {
    operator fun invoke(gameId: Long): Flow<List<PlayerColor>> =
        playsRepository.observeColorsUsedForGame(gameId).map { colorStrings ->
            colorStrings
                .mapNotNull { PlayerColor.fromString(it) }
                .sortedBy { it.ordinal }
        }
}
