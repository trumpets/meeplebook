package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.plays.model.Play
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes the most recent plays from the collection.
 *
 * Emits a flow of plays sorted by date, with the most recent first.
 * Updates whenever plays are added, modified, or removed from the database.
 */
class ObserveRecentPlaysUseCase @Inject constructor(
    private val playsRepository: PlaysRepository
) {
    /**
     * Returns a flow of the most recent plays up to the specified limit.
     *
     * @param limit Maximum number of plays to return (default: 5)
     */
    operator fun invoke(limit: Int = 5): Flow<List<Play>> {
        // Use optimized Room query with LIMIT instead of loading all plays
        return playsRepository.observeRecentPlays(limit)
    }
}