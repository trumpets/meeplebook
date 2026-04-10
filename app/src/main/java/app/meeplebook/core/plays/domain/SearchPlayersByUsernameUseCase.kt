package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.PlaysRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Searches distinct players whose username contains [query].
 *
 * Returns a reactive stream of matching [PlayerIdentity] values for live
 * autocomplete in the Add/Edit Player dialog.
 *
 * @param query Substring to match against BGG usernames.
 */
class SearchPlayersByUsernameUseCase @Inject constructor(
    private val playsRepository: PlaysRepository
) {
    operator fun invoke(query: String): Flow<List<PlayerIdentity>> =
        playsRepository.searchPlayersByUsername(query)
}
