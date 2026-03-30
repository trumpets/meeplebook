package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.PlaysRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes player identity suggestions for a given play location.
 *
 * Returns players who have previously played at [location], ordered by the
 * repository's frequency ranking. Callers are responsible for mapping to
 * any UI-specific suggestion model (e.g., adding a `playCount` display field).
 *
 * @param location The current location string typed by the user.
 */
class ObservePlayerSuggestionsUseCase @Inject constructor(
    private val playsRepository: PlaysRepository
) {
    operator fun invoke(location: String): Flow<List<PlayerIdentity>> =
        playsRepository.observePlayersByLocation(location)
}
