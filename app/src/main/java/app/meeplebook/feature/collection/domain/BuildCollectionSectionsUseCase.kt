package app.meeplebook.feature.collection.domain

import app.meeplebook.core.collection.domain.DomainCollectionItem
import javax.inject.Inject

/**
 * Organizes collection items into alphabetical sections for display.
 *
 * Groups games by the first letter of their name (case-insensitive), creating
 * sections A-Z. Games with names starting with non-alphabetic characters are
 * grouped into a '#' section. Sections are sorted with the '#' section appearing
 * first, followed by alphabetical sections A-Z.
 */
class BuildCollectionSectionsUseCase @Inject constructor() {

    /**
     * Organizes the provided collection items into sections.
     *
     * @param items The collection items to organize into sections
     * @return A list of [DomainCollectionSection] sorted with '#' first, then A-Z
     */
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