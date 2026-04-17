package app.meeplebook.feature.plays

import androidx.annotation.StringRes
import app.meeplebook.R
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.ui.UiText
import java.time.YearMonth

/**
 * Display-facing UI state for the Plays screen.
 *
 * Unlike [PlaysBaseState], this sealed model is not mutated directly by the reducer. It is derived
 * by combining reducer-owned state with observed domain screen data.
 */
sealed interface PlaysUiState {

    /** Initial state shown before the first combined screen data arrives. */
    data object Loading : PlaysUiState

    /**
     * Empty state shown when there are no plays to display.
     *
     * @property reason The reason why the screen is empty (no plays vs. no search results).
     */
    data class Empty(
        val reason: EmptyReason,
        val common: PlaysCommonState
    ) : PlaysUiState

    /**
     * Content state with plays grouped by month/year.
     *
     * @property sections List of play sections, each representing plays from a specific month.
     */
    data class Content(
        val sections: List<PlaysSection>,
        val common: PlaysCommonState
    ) : PlaysUiState

    /**
     * Error state shown when data loading fails.
     *
     * @property errorMessageUiText UiText representing the error message to display.
     */
    data class Error(
        val errorMessageUiText: UiText,
        val common: PlaysCommonState
    ) : PlaysUiState
}

/**
 * Common display properties shared by non-loading [PlaysUiState] variants.
 *
 * @property searchQuery Current search query shown in the UI.
 * @property playStats Aggregated statistics about the user's plays.
 * @property isRefreshing Whether a refresh operation is currently in progress.
 */
data class PlaysCommonState(
    val searchQuery: String = "",
    val playStats: PlayStats = PlayStats(),
    val isRefreshing: Boolean = false
)

/**
 * Reasons why the Plays screen is empty.
 *
 * @property descriptionResId String resource ID to show in the empty-state body.
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
 * UI section of plays grouped by month/year.
 *
 * @property monthYearDate The month/year represented by this section.
 * @property plays Play rows rendered beneath the section header.
 */
data class PlaysSection(
    val monthYearDate: YearMonth,
    val plays: List<PlayItem>
)

/**
 * Render-ready representation of a single play row.
 *
 * @property playId Unique identifier for the play.
 * @property gameName Name of the played game.
 * @property thumbnailUrl Optional thumbnail URL for the game.
 * @property dateUiText Formatted date shown to the user.
 * @property durationUiText Formatted duration shown to the user.
 * @property playerSummaryUiText Formatted summary of participating players.
 * @property location Optional location where the play happened.
 * @property comments Optional comments shown in the row.
 * @property syncStatus Current sync status with BGG.
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
 * Summary statistics displayed at the top of the Plays screen.
 *
 * @property uniqueGamesCount Total number of unique games played.
 * @property totalPlays Total number of recorded plays.
 * @property playsThisYear Number of plays recorded in [currentYear].
 * @property currentYear The current year used for the yearly count.
 */
data class PlayStats(
    val uniqueGamesCount: Long = 0,
    val totalPlays: Long = 0,
    val playsThisYear: Long = 0,
    val currentYear: Int = 0
)
