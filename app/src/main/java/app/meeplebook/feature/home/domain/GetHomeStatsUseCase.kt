package app.meeplebook.feature.home.domain

import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.feature.home.HomeStats
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case that calculates home screen statistics from collection and plays data.
 * Uses optimized Room queries for better performance.
 */
class GetHomeStatsUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val playsRepository: PlaysRepository
) {
    /**
     * Calculates statistics for the home screen using optimized database queries.
     *
     * @return [HomeStats] containing games count, play counts, and unplayed count
     */
    suspend operator fun invoke(): HomeStats {
        // Use optimized Room queries instead of loading all data into memory
        val gamesCount = collectionRepository.getCollectionCount()
        val totalPlays = playsRepository.getTotalPlaysCount()
        
        // Calculate plays this month using Room query
        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val playsThisMonth = playsRepository.getPlaysCountForMonth(currentMonth)
        
        // Use optimized Room query for unplayed games count
        val unplayedCount = collectionRepository.getUnplayedGamesCount()
        
        return HomeStats(
            gamesCount = gamesCount,
            totalPlays = totalPlays,
            playsThisMonth = playsThisMonth,
            unplayedCount = unplayedCount
        )
    }
}
