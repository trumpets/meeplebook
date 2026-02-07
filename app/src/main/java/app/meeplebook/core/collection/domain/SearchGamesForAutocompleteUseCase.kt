package app.meeplebook.core.collection.domain

import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.collection.model.GameSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for searching games in the collection for autocomplete.
 * Returns matching games from the user's collection.
 */
class SearchGamesForAutocompleteUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository
) {
    /**
     * Searches for games matching the query.
     *
     * @param query The search query.
     * @param limit Maximum number of results to return.
     * @return Flow emitting list of matching [GameSummary].
     */
    operator fun invoke(query: String, limit: Int = 20): Flow<List<GameSummary>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
        } else {
            val games = collectionRepository.searchGamesForAutocomplete(query, limit)
            emit(games)
        }
    }
}
