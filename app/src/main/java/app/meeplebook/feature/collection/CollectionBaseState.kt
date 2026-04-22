package app.meeplebook.feature.collection

import app.meeplebook.core.collection.model.CollectionSort
import app.meeplebook.core.collection.model.QuickFilter

/**
 * Reducer-owned base state for the Collection screen.
 *
 * This state owns the synchronous, UI-visible inputs for the feature:
 * - raw search query text
 * - selected quick filter
 * - selected sort order
 * - selected view mode
 * - sort-sheet visibility
 *
 * `CollectionViewModel` derives debounced query flows from this state and combines it with external
 * collection data to produce the renderable [CollectionUiState].
 */
data class CollectionBaseState(
    val searchQuery: String = "",
    val quickFilter: QuickFilter = QuickFilter.ALL,
    val sort: CollectionSort = CollectionSort.ALPHABETICAL,
    val viewMode: CollectionViewMode = CollectionViewMode.LIST,
    val isSortSheetVisible: Boolean = false,
)
