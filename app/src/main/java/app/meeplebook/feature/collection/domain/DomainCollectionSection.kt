package app.meeplebook.feature.collection.domain

import app.meeplebook.core.collection.domain.DomainCollectionItem

/**
 * Represents a section of collection items grouped by an alphabetical key.
 *
 * Used to organize the collection display into alphabetically sorted sections,
 * where each section contains items that start with the same letter (or '#' for non-alphabetic).
 *
 * @property key The section identifier - a letter ('A'-'Z') or '#' for non-alphabetic names.
 * @property items The list of collection items belonging to this section.
 */
data class DomainCollectionSection(
    val key: Char,
    val items: List<DomainCollectionItem>
)