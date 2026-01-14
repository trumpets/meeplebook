package app.meeplebook.feature.collection

import androidx.annotation.StringRes
import app.meeplebook.R
import app.meeplebook.core.collection.model.QuickFilter
import app.meeplebook.core.collection.model.CollectionSort

/**
 * UI state for the Collection screen.
 */
sealed interface CollectionUiState {

    data object Loading : CollectionUiState

    data class Empty(
        val reason: EmptyReason,
        val searchQuery: String,
        val activeQuickFilter: QuickFilter,
        val totalGameCount: Int
    ) : CollectionUiState

    data class Content(
        val searchQuery: String,

        // Presentation
        val viewMode: CollectionViewMode, // GRID or LIST
        val sort: CollectionSort,
        val activeQuickFilter: QuickFilter,

        val availableSortOptions: List<CollectionSort>,

        // Data
        val sections: List<CollectionSection>,
        val sectionIndices: Map<Char, Int>,
        val totalGameCount: Int,

        // UI chrome
        val isRefreshing: Boolean,
        val showAlphabetJump: Boolean,
        val isSortSheetVisible: Boolean
    ) : CollectionUiState

    data class Error(
        @StringRes val errorMessageResId: Int
    ) : CollectionUiState
}

enum class CollectionViewMode {
    GRID, LIST
}

enum class EmptyReason(
    @StringRes val descriptionResId: Int
) {
    NO_GAMES(R.string.collection_empty),
    NO_SEARCH_RESULTS(R.string.collection_search_no_results),
    NO_FILTER_RESULTS(R.string.collection_filter_no_results)
}

data class CollectionSection(
    val key: Char,
    val games: List<CollectionGameItem>
)

data class CollectionGameItem(
    val gameId: Long,
    val name: String,
    val yearPublished: Int?,
    val thumbnailUrl: String?,

    // Subtitles
    val playsSubtitle: String, // "42 plays"
    val playersSubtitle: String,   // "2–4p"
    val playTimeSubtitle: String, // "30–60m"

    // Flags
    val isNew: Boolean
)

/**
 * One-time UI effects for the Collection screen.
 *
 * Unlike [CollectionUiState], which represents the continuous state of the screen,
 * UI effects are one-time events that trigger side effects such as navigation,
 * scrolling, or showing dialogs. Effects are emitted via a SharedFlow and consumed
 * once by the UI layer.
 */
sealed interface CollectionUiEffects {
    data class ScrollToLetter(val letter: Char) : CollectionUiEffects
    data class NavigateToGame(val gameId: Long) : CollectionUiEffects
    data object OpenSortSheet : CollectionUiEffects
    data object DismissSortSheet : CollectionUiEffects
}