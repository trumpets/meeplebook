package app.meeplebook.feature.home.domain

import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.feature.home.RecentPlay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Use case that retrieves and formats recent plays for the home screen.
 */
class GetRecentPlaysUseCase @Inject constructor(
    private val playsRepository: PlaysRepository
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
                    dateText = formatDateText(play.date),
                    playerCount = play.players.size,
                    playerNames = formatPlayerNames(play.players.map { it.name })
                )
            }
    }
    
    /**
     * Formats a date string (YYYY-MM-DD) into a human-readable text.
     */
    private fun formatDateText(dateString: String): String {
        val playDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()
        val daysDiff = ChronoUnit.DAYS.between(playDate, today)
        
        return when {
            daysDiff == 0L -> "Today"
            daysDiff == 1L -> "Yesterday"
            daysDiff < 7L -> "$daysDiff days ago"
            else -> playDate.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    }
    
    /**
     * Formats a list of player names into a comma-separated string.
     */
    private fun formatPlayerNames(names: List<String>): String {
        return when {
            names.isEmpty() -> "No players"
            names.size <= 3 -> names.joinToString(", ")
            else -> "${names.take(3).joinToString(", ")}, +${names.size - 3}"
        }
    }
}
