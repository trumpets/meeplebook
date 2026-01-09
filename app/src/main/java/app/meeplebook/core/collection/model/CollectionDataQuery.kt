package app.meeplebook.core.collection.model

/**
 * Represents a query to filter and sort a user's collection.
 *
 * This class encapsulates all parameters needed to filter and sort collection items,
 * including text-based search, predefined filters, and sort options.
 *
 * @param searchQuery Text query to search for games by name. Empty string means no text filter.
 * @param quickFilter Predefined filter to apply (e.g., show all games or only unplayed games).
 * @param sort Sort order to apply to the filtered collection.
 */
data class CollectionDataQuery(
    val searchQuery: String,
    val quickFilter: QuickFilter,
    val sort: CollectionSort
)