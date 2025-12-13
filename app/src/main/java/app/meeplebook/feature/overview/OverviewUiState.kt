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
    @StringRes val subtitleResId: Int
)

/**
 * Statistics summary shown on the overview screen.
 */
data class HomeStats(
    val gamesCount: Int = 0,
    val totalPlays: Int = 0,
    val playsThisMonth: Int = 0,
    val unplayedCount: Int = 0
)

/**
 * UI state for the Overview screen (home tab).
 */
data class OverviewUiState(
    val stats: HomeStats = HomeStats(),
    val recentPlays: List<RecentPlay> = emptyList(),
    val recentlyAddedGame: GameHighlight? = null,
    val suggestedGame: GameHighlight? = null,
    val lastSyncedText: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    @StringRes val errorMessageResId: Int? = null
)
