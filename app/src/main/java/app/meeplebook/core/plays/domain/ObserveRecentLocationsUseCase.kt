package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.PlaysRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes the user's most recently used play locations.
 *
 * Emits a reactive stream of location strings ordered by recency so the Add Play
 * screen can display quick-pick chips without any user input.
 */
class ObserveRecentLocationsUseCase @Inject constructor(
    private val playsRepository: PlaysRepository
) {
    operator fun invoke(): Flow<List<String>> =
        playsRepository.observeRecentLocations()
}
