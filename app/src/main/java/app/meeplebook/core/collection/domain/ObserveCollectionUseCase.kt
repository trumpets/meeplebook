package app.meeplebook.core.collection.domain

import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.collection.model.CollectionDataQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Observes the users collection.
 *
 * Emits a flow of [DomainCollectionItem] containing all the games
 * in the user's collection.
 */
class ObserveCollectionUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository
) {

    operator fun invoke(query: CollectionDataQuery? = null): Flow<List<DomainCollectionItem>> {
        return collectionRepository.observeCollection(query).map { items ->
            items.map { item -> item.toDomainCollectionItem() }
        }
    }
}