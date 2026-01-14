package app.meeplebook.feature.overview

import androidx.annotation.StringRes

/**
 * Represents a recently played game entry.
 */
data class RecentPlay(
    val id: Long,
    val gameName: String,
    val thumbnailUrl: String?,
    val dateText: String,
    val playerCount: Int,
    val playerNames: String
)

/**
 * Represents a game highlight (recently added or suggestion).
 */
data class GameHighlight(
    val id: Long,
    val gameName: String,
    val thumbnailUrl: String?,
    val subtitleText: String
)

/**
 * Statistics summary shown on the overview screen.
 */
data class OverviewStats(
    val gamesCount: Long = 0,
    val totalPlays: Long = 0,
    val playsThisMonth: Long = 0,
    val unplayedCount: Long = 0
)

/**
 * UI state for the Overview screen.
 */
data class OverviewUiState(
    val stats: OverviewStats = OverviewStats(),
    val recentPlays: List<RecentPlay> = emptyList(),
    val recentlyAddedGame: GameHighlight? = null,
    val suggestedGame: GameHighlight? = null,
    val lastSyncedText: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    @StringRes val errorMessageResId: Int? = null
)

/**
 * Side effects for the Overview screen.
 *
 * Represents transient UI states that don't persist across configuration changes,
 * such as loading indicators and temporary error messages.
 */
data class OverviewUiEffects(
    val isRefreshing: Boolean = false,
    @StringRes val errorMessageResId: Int? = null
)
