package app.meeplebook.feature.overview

import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.uiTextEmpty

/**
 * Renderable Overview screen state.
 *
 * Overview follows the repo's screen-state pattern: the screen is either [Loading], full-screen
 * [Error], or [Content]. Reducer-owned transient values such as refresh/error bookkeeping live in
 * [OverviewBaseState]; this type is the final UI-facing state produced by combining base state with
 * the observed domain overview stream.
 */
sealed interface OverviewUiState {
    /**
     * Initial state shown before the first overview snapshot is available.
     */
    data object Loading : OverviewUiState

    /**
     * Full-screen failure state shown when refresh work fails.
     */
    data class Error(
        val errorMessageUiText: UiText
    ) : OverviewUiState

    /**
     * Main Overview content with stats, recent plays, collection highlights, and refresh state.
     */
    data class Content(
        val stats: OverviewStats = OverviewStats(),
        val recentPlays: List<RecentPlay> = emptyList(),
        val recentlyAddedGame: GameHighlight? = null,
        val suggestedGame: GameHighlight? = null,
        val lastSyncedUiText: UiText = uiTextEmpty(),
        val isRefreshing: Boolean = false
    ) : OverviewUiState
}

/**
 * UI model for a recent play card on the Overview screen.
 */
data class RecentPlay(
    val playId: PlayId,
    val gameName: String,
    val thumbnailUrl: String?,
    val dateUiText: UiText,
    val playerCount: Int,
    val playerNamesUiText: UiText
)

/**
 * UI model for a single collection highlight card.
 */
data class GameHighlight(
    val id: Long,
    val gameName: String,
    val thumbnailUrl: String?,
    val subtitleUiText: UiText
)

/**
 * Summary stats displayed in the overview stats card.
 */
data class OverviewStats(
    val gamesCount: Long = 0,
    val totalPlays: Long = 0,
    val playsThisMonth: Long = 0,
    val unplayedCount: Long = 0
)
