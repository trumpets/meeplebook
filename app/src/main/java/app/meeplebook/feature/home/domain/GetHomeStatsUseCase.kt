package app.meeplebook.feature.home.domain

import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.feature.home.HomeStats
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case that calculates home screen statistics from collection and plays data.
 */
class GetHomeStatsUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val playsRepository: PlaysRepository
) {
    /**
     * Calculates statistics for the home screen.
     *
     * @return [HomeStats] containing games count, play counts, and unplayed count
     */
    suspend operator fun invoke(): HomeStats {
        val collection = collectionRepository.getCollection()
        val plays = playsRepository.getPlays()
        
        val gamesCount = collection.size
        val totalPlays = plays.sumOf { it.quantity }
        
        // Calculate plays this month
        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val playsThisMonth = plays
            .filter { it.date.startsWith(currentMonth) }
            .sumOf { it.quantity }
        
        // Calculate unplayed games (games in collection that have no plays)
        val playedGameIds = plays.map { it.gameId }.toSet()
        val unplayedCount = collection.count { it.gameId !in playedGameIds }
        
        return HomeStats(
            gamesCount = gamesCount,
            totalPlays = totalPlays,
            playsThisMonth = playsThisMonth,
            unplayedCount = unplayedCount
        )
    }
}
