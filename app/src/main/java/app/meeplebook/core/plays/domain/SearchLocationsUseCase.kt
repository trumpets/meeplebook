package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.PlaysRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Searches play locations matching the given query string.
 *
 * Returns a reactive stream that updates whenever the underlying location data
 * changes, enabling live autocomplete behaviour in the Add Play screen.
 *
 * @param query Text to match against location names. An empty string returns all locations.
 */
class SearchLocationsUseCase @Inject constructor(
    private val playsRepository: PlaysRepository
) {
    operator fun invoke(query: String): Flow<List<String>> =
        playsRepository.observeLocations(query)
}
