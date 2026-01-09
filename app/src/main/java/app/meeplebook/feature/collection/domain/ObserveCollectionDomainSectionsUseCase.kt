package app.meeplebook.feature.collection.domain

import app.meeplebook.core.collection.domain.ObserveCollectionUseCase
import app.meeplebook.core.collection.model.CollectionDataQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveCollectionDomainSectionsUseCase @Inject constructor(
    private val observeCollection: ObserveCollectionUseCase,
    private val sectionBuilder: BuildCollectionSectionsUseCase
) {

    operator fun invoke(
        query: CollectionDataQuery
    ): Flow<List<DomainCollectionSection>> {
        return observeCollection(query)
            .map(sectionBuilder::invoke)
    }
}