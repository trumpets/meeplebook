package app.meeplebook.feature.collection

/**
 * Represents user actions and system events for the Collection screen.
 * 
 * These events are emitted from the UI layer and processed by the ViewModel
 * to update the collection state, including search, filtering, sorting, and navigation.
 */
sealed interface CollectionEvent {

    // Search
    data class SearchChanged(val query: String) : CollectionEvent

    // Quick filters
    data class QuickFilterSelected(val filter: QuickFilter) : CollectionEvent

    // View / sort
    data class ToggleViewMode(val viewMode: CollectionViewMode) : CollectionEvent
    data class SortSelected(val sort: CollectionSort) : CollectionEvent

    // Bottom sheet
    data object OpenSortSheet : CollectionEvent
    data object DismissSortSheet : CollectionEvent

    // Actions
    data class GameClicked(val gameId: Int) : CollectionEvent
    data class LogPlayClicked(val gameId: Int) : CollectionEvent

    // System
    data object Refresh : CollectionEvent
    data class JumpToLetter(val letter: Char) : CollectionEvent
}