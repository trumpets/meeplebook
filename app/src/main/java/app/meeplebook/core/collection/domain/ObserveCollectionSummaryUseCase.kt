package app.meeplebook.core.collection.domain

import app.meeplebook.core.collection.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Observes collection statistics.
 *
 * Emits statistics including total games owned, and count of unplayed games.
 * Updates whenever the underlying collection data changes in the database.
 */
class ObserveCollectionSummaryUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository
) {
    operator fun invoke(): Flow<DomainCollectionSummary> =
        combine(
            collectionRepository.observeCollectionCount(),
            collectionRepository.observeUnplayedGamesCount()
        ) { total, unplayed ->
            DomainCollectionSummary(
                totalGames = total,
                unplayedGames = unplayed
            )
        }
}