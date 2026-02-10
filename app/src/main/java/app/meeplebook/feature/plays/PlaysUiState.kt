package app.meeplebook.feature.plays

import androidx.annotation.StringRes
import app.meeplebook.R
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.ui.UiText
import java.time.YearMonth

/**
 * UI state for the Plays screen.
 */
sealed interface PlaysUiState {

    /** Common state shared across all UI state variants. */
    val common: PlaysCommonState

    /** Loading state shown while initial data is being fetched. */
    data object Loading : PlaysUiState {
        override val common = PlaysCommonState()
    }

    /**
     * Empty state shown when there are no plays to display.
     *
     * @property reason The reason why the screen is empty (no plays vs. no search results).
     */
    data class Empty(
        val reason: EmptyReason,
        override val common: PlaysCommonState
    ) : PlaysUiState

    /**
     * Content state with plays grouped by month/year.
     *
     * @property sections List of play sections, each representing plays from a specific month.
     */
    data class Content(
        val sections: List<PlaysSection>,
        override val common: PlaysCommonState
    ) : PlaysUiState

    /**
     * Error state shown when data loading fails.
     *
     * @property errorMessageUiText UiText representing the error message to display.
     */
    data class Error(
        val errorMessageUiText: UiText,
        override val common: PlaysCommonState
    ) : PlaysUiState
}

/**
 * Common state shared across all [PlaysUiState] variants.
 *
 * This includes properties that are always present regardless of whether
 * the screen is loading, empty, showing content, or displaying an error.
 *
 * @property searchQuery Current search query text entered by the user.
 * @property playStats Aggregated statistics about the user's plays.
 * @property isRefreshing Whether a refresh operation is currently in progress.
 */
data class PlaysCommonState(
    val searchQuery: String = "",
    val playStats: PlayStats = PlayStats(),
    val isRefreshing: Boolean = false
)

/**
 * Reasons why the plays screen might be empty.
 *
 * @property descriptionResId String resource ID for the description to show to the user.
 */
enum class EmptyReason(
    @StringRes val descriptionResId: Int
) {
    /** User has no recorded plays in their collection. */
    NO_PLAYS(R.string.plays_empty),

    /** Search query returned no matching plays. */
    NO_SEARCH_RESULTS(R.string.plays_search_no_results),
}

/**
 * A section of plays grouped by month/year.
 *
 * @property monthYearDate The month/year that this section represents.
 * @property plays List of play items recorded during this month.
 */
data class PlaysSection(
    val monthYearDate: YearMonth,
    val plays: List<PlayItem>
)

/**
 * Represents a single play record in the UI.
 *
 * @property playId Unique identifier for the play record.
 * @property gameName Name of the game that was played.
 * @property thumbnailUrl Optional URL to the game's thumbnail image.
 * @property dateUiText Formatted date text for display (e.g., "27/01/2026").
 * @property durationUiText Formatted duration text for display (e.g., "120 min").
 * @property playerSummaryUiText Summary of players who participated (e.g., "3 players").
 * @property location Optional location where the game was played.
 * @property comments Optional user comments about the play.
 * @property syncStatus Current synchronization status with BGG.
 */
data class PlayItem(
    val playId: PlayId,
    val gameName: String,
    val thumbnailUrl: String?,
    val dateUiText: UiText,
    val durationUiText: UiText,
    val playerSummaryUiText: UiText,
    val location: String?,
    val comments: String?,
    val syncStatus: PlaySyncStatus
)

/**
 * Statistics summary shown on the plays screen.
 *
 * @property uniqueGamesCount Total number of unique games played.
 * @property totalPlays Total number of play records.
 * @property playsThisYear Number of plays recorded in the current year.
 * @property currentYear The current year for which [playsThisYear] is calculated.
 */
data class PlayStats(
    val uniqueGamesCount: Long = 0,
    val totalPlays: Long = 0,
    val playsThisYear: Long = 0,
    val currentYear: Int = 0
)

/**
 * One-time UI effects for the Plays screen.
 *
 * Unlike [PlaysUiState], which represents the continuous state of the screen,
 * UI effects are one-time events that trigger side effects such as navigation,
 * scrolling, or showing dialogs. Effects are emitted via a SharedFlow and consumed
 * once by the UI layer.
 */
sealed interface PlaysUiEffects {
    /**
     * Navigate to the details screen for a specific play.
     *
     * @property playId The ID of the play to view.
     */
    data class NavigateToPlay(val playId: PlayId) : PlaysUiEffects

    /**
     * Show a temporary snackbar message to the user.
     *
     * @property messageUiText The message to display.
     */
    data class ShowSnackbar(val messageUiText: UiText) : PlaysUiEffects
}