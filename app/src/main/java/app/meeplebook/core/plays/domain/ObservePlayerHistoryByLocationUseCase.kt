package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.plays.model.PlayerHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for observing player history at a specific location.
 * Returns players who have played at this location, sorted by play count.
 */
class ObservePlayerHistoryByLocationUseCase @Inject constructor(
    private val playsRepository: PlaysRepository
) {
    /**
     * Observes player history for a specific location.
     *
     * @param location The location to query.
     * @return Flow emitting list of [PlayerHistory] sorted by play count (descending).
     */
    operator fun invoke(location: String): Flow<List<PlayerHistory>> = flow {
        val playerHistory = playsRepository.getPlayerHistoryByLocation(location)
        emit(playerHistory)
    }
}
