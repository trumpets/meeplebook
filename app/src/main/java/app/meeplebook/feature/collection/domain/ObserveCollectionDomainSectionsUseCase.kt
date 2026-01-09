package app.meeplebook.feature.collection.domain

import app.meeplebook.core.collection.domain.ObserveCollectionUseCase
import app.meeplebook.core.collection.model.CollectionDataQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case that exposes the user's board game collection as a stream of
 * [DomainCollectionSection]s suitable for presentation.
 *
 * This wraps [ObserveCollectionUseCase], which provides the raw collection data
 * for the given [CollectionDataQuery], and transforms each emission using
 * [BuildCollectionSectionsUseCase] to group, sort, or otherwise organize items
 * into domain-level sections.
 *
 * The returned [Flow] will emit a new list of sections whenever the underlying
 * collection data changes.
 */
class ObserveCollectionDomainSectionsUseCase @Inject constructor(
    private val observeCollection: ObserveCollectionUseCase,
    private val sectionBuilder: BuildCollectionSectionsUseCase
) {

    /**
     * Observes the collection matching the given [query] and maps it into a list
     * of [DomainCollectionSection]s.
     */
    operator fun invoke(
        query: CollectionDataQuery
    ): Flow<List<DomainCollectionSection>> {
        return observeCollection(query)
            .map(sectionBuilder::invoke)
    }
}