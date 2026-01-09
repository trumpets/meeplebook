package app.meeplebook.core.collection.domain

import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.collection.model.CollectionDataQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Observes the user's collection.
 *
 * Emits a flow of [DomainCollectionItem] containing all the games
 * in the user's collection.
 *
 * @param query Optional [CollectionDataQuery] describing filters and sorting to apply.
 * If `null`, a default query is used which returns the full collection with the
 * repository's default sorting and filtering.
 */
class ObserveCollectionUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository
) {

    operator fun invoke(query: CollectionDataQuery? = null): Flow<List<DomainCollectionItem>> {
        val effectiveQuery = query ?: CollectionDataQuery()
        return collectionRepository.observeCollection(effectiveQuery).map { items ->
            items.map { item -> item.toDomainCollectionItem() }
        }
    }
}