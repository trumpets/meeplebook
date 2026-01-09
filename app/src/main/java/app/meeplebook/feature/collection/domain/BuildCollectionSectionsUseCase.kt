package app.meeplebook.feature.collection.domain

import app.meeplebook.core.collection.domain.DomainCollectionItem
import javax.inject.Inject

class BuildCollectionSectionsUseCase @Inject constructor() {

    operator fun invoke(
        items: List<DomainCollectionItem>
    ): List<DomainCollectionSection> {
        return items
            .groupBy { it.sectionKey() }
            .toSortedMap(
                compareBy<Char> { it != '#' }
                    .thenBy { it }
            )
            .map { (key, items) ->
                DomainCollectionSection(
                    key = key,
                    items = items
                )
            }
    }

    private fun DomainCollectionItem.sectionKey(): Char {
        val first = name.firstOrNull() ?: return '#'
        val upper = first.uppercaseChar()
        return if (upper in 'A'..'Z') upper else '#'
    }
}