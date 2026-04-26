package app.meeplebook.feature.collection

import app.meeplebook.core.collection.model.CollectionSort
import app.meeplebook.core.collection.model.QuickFilter

/**
 * Events emitted by the Collection UI and handled by the reducer/effect pipeline.
 *
 * The nested event groups mirror the feature's responsibilities:
 * - [SearchEvent] updates raw query text
 * - [FilterEvent] updates active quick filters
 * - [DisplayEvent] changes persistent presentation state
 * - [SortSheetEvent] toggles reducer-owned sort-sheet visibility
 * - [ActionEvent] triggers navigation, scrolling, or async work
 */
sealed interface CollectionEvent {

    /** Events related to the search field. */
    sealed interface SearchEvent : CollectionEvent {
        /** User changed the raw search query shown in the UI. */
        data class SearchChanged(val query: String) : SearchEvent
    }

    /** Events related to quick-filter selection. */
    sealed interface FilterEvent : CollectionEvent {
        /** User selected a different quick filter. */
        data class QuickFilterSelected(val filter: QuickFilter) : FilterEvent
    }

    /** Events that change the persistent display configuration. */
    sealed interface DisplayEvent : CollectionEvent {
        /** User switched between list and grid presentation. */
        data class ViewModeSelected(val viewMode: CollectionViewMode) : DisplayEvent

        /** User chose a different sort order for the collection data. */
        data class SortSelected(val sort: CollectionSort) : DisplayEvent
    }

    /** Events that control reducer-owned sort-sheet visibility. */
    sealed interface SortSheetEvent : CollectionEvent {
        /** Show the sort bottom sheet. */
        data object OpenSortSheet : SortSheetEvent

        /** Hide the sort bottom sheet. */
        data object DismissSortSheet : SortSheetEvent
    }

    /** Events that trigger one-shot UI work or domain work. */
    sealed interface ActionEvent : CollectionEvent {
        /** Signals that the Collection screen entered composition and should run screen-open work. */
        data object ScreenOpened : ActionEvent

        /** Navigate to a collection game's detail screen. */
        data class GameClicked(val gameId: Long) : ActionEvent

        /** Start logging a play for a collection game. */
        data class LogPlayClicked(val gameId: Long) : ActionEvent

        /** Refresh the collection from remote data. */
        data object Refresh : ActionEvent

        /** Scroll the collection list/grid to the section for [letter]. */
        data class JumpToLetter(val letter: Char) : ActionEvent
    }
}
