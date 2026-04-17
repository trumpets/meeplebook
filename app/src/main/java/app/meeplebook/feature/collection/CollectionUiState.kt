package app.meeplebook.feature.collection

import androidx.annotation.StringRes
import app.meeplebook.R
import app.meeplebook.core.collection.model.CollectionSort
import app.meeplebook.core.collection.model.QuickFilter
import app.meeplebook.core.ui.UiText

/**
 * Renderable UI state for the Collection screen.
 *
 * `CollectionViewModel` derives this state by combining reducer-owned [CollectionBaseState] with
 * observed collection data and summary statistics. The reducer never mutates this sealed state
 * directly; it owns [CollectionBaseState] instead.
 */
sealed interface CollectionUiState {

    /** The screen is still waiting for the first combined emission. */
    data object Loading : CollectionUiState

    /** The collection is empty for the given [reason], while preserving shared chrome in [common]. */
    data class Empty(
        val reason: EmptyReason,
        val common: CollectionCommonState
    ) : CollectionUiState

    /**
     * The collection has visible sections to render.
     *
     * Content-specific presentation values such as [viewMode], [sort], alphabet jump visibility, and
     * sort-sheet visibility are copied from reducer-owned base state or derived data.
     */
    data class Content(
        val viewMode: CollectionViewMode, // GRID or LIST
        val sort: CollectionSort,
        val availableSortOptions: List<CollectionSort>,
        val sections: List<CollectionSection>,
        val sectionIndices: Map<Char, Int>,
        val showAlphabetJump: Boolean,
        val isSortSheetVisible: Boolean,
        val common: CollectionCommonState
    ) : CollectionUiState

    /** The screen hit a non-recoverable presentation error while still showing shared chrome. */
    data class Error(
        val errorMessageUiText: UiText,
        val common: CollectionCommonState
    ) : CollectionUiState
}

/**
 * Shared chrome/state used across multiple collection render states.
 */
data class CollectionCommonState(
    val searchQuery: String = "",
    val activeQuickFilter: QuickFilter = QuickFilter.ALL,
    val totalGameCount: Long = 0L,
    val unplayedGameCount: Long = 0L,
    val isRefreshing: Boolean = false
)

/** Available layouts for rendering collection items. */
enum class CollectionViewMode {
    GRID, LIST
}

/** Reasons the collection can render an empty state. */
enum class EmptyReason(
    @StringRes val descriptionResId: Int
) {
    NO_GAMES(R.string.collection_empty),
    NO_SEARCH_RESULTS(R.string.collection_search_no_results),
    NO_FILTER_RESULTS(R.string.collection_filter_no_results)
}

/** One alphabetic collection section and the games it contains. */
data class CollectionSection(
    val key: Char,
    val games: List<CollectionGameItem>
)

/** Renderable game row/card data for the Collection screen. */
data class CollectionGameItem(
    val gameId: Long,
    val name: String,
    val yearPublished: Int?,
    val thumbnailUrl: String?,
    val playsSubtitleUiText: UiText, // "42 plays"
    val playersSubtitleUiText: UiText,   // "2–4p"
    val playTimeSubtitleUiText: UiText, // "30–60m"
    val isUnplayed: Boolean
)
