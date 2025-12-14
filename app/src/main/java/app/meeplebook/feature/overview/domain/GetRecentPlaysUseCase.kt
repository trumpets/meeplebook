package app.meeplebook.feature.overview.domain

import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.feature.overview.RecentPlay
import javax.inject.Inject

/**
 * Use case that retrieves and formats recent plays for the overview screen.
 * Uses optimized Room query for better performance.
 */
class GetRecentPlaysUseCase @Inject constructor(
    private val playsRepository: PlaysRepository
) {
    /**
     * Gets the most recent plays, formatted for display.
     * Uses optimized database query with LIMIT for better performance.
     *
     * @param limit Maximum number of recent plays to return (default: 5)
     * @return List of [RecentPlay] sorted by date (most recent first)
     */
    suspend operator fun invoke(limit: Int = 5): List<RecentPlay> {
        // Use optimized Room query with LIMIT instead of loading all plays
        val plays = playsRepository.getRecentPlays(limit)

        return plays.map { play ->
            RecentPlay(
                id = play.id.toLong(),
                gameName = play.gameName,
                thumbnailUrl = null, // TODO: Add thumbnail support from CollectionRepository by mapping Play.gameId to collection items
                date = play.date,
                playerCount = play.players.size,
                playerNames = play.players.map { it.name }
            )
        }
    }
}