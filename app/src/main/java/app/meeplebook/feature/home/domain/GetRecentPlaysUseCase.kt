package app.meeplebook.feature.home.domain

import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.util.DateFormatter
import app.meeplebook.feature.home.RecentPlay
import javax.inject.Inject

/**
 * Use case that retrieves and formats recent plays for the home screen.
 */
class GetRecentPlaysUseCase @Inject constructor(
    private val playsRepository: PlaysRepository,
    private val dateFormatter: DateFormatter
) {
    /**
     * Gets the most recent plays, formatted for display.
     *
     * @param limit Maximum number of recent plays to return (default: 5)
     * @return List of [RecentPlay] sorted by date (most recent first)
     */
    suspend operator fun invoke(limit: Int = 5): List<RecentPlay> {
        val plays = playsRepository.getPlays()
        
        return plays
            .sortedByDescending { it.date }
            .take(limit)
            .map { play ->
                RecentPlay(
                    id = play.id.toLong(),
                    gameName = play.gameName,
                    thumbnailUrl = null, // TODO: Add thumbnail support from CollectionRepository by mapping Play.gameId to collection items
                    dateText = dateFormatter.formatDateText(play.date),
                    playerCount = play.players.size,
                    playerNames = dateFormatter.formatPlayerNames(play.players.map { it.name })
                )
            }
    }
}
