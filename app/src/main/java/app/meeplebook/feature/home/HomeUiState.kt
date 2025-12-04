package app.meeplebook.feature.home

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
    val subtitle: String
)

/**
 * Statistics summary shown on the home screen.
 */
data class HomeStats(
    val gamesCount: Int = 0,
    val totalPlays: Int = 0,
    val playsThisMonth: Int = 0,
    val unplayedCount: Int = 0
)

/**
 * UI state for the Home screen.
 */
data class HomeUiState(
    val stats: HomeStats = HomeStats(),
    val recentPlays: List<RecentPlay> = emptyList(),
    val recentlyAddedGame: GameHighlight? = null,
    val suggestedGame: GameHighlight? = null,
    val lastSyncedText: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)
