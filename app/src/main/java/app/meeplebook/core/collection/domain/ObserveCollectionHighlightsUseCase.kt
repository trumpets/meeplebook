package app.meeplebook.core.collection.domain

import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.collection.model.CollectionHighlights
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Observes game highlights for the collection overview screen.
 *
 * Emits a flow of [CollectionHighlights] containing the most recently added game
 * and the first unplayed game as a suggested play. Both values may be null if
 * no matching games exist in the collection.
 */
class ObserveCollectionHighlightsUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository
) {

    operator fun invoke(): Flow<CollectionHighlights> {
        return combine(
            collectionRepository.observeMostRecentlyAddedItem(),
            collectionRepository.observeFirstUnplayedGame()
        ) { recentlyAdded, suggested ->
            CollectionHighlights(
                recentlyAdded = recentlyAdded,
                suggested = suggested
            )
        }
    }
}
