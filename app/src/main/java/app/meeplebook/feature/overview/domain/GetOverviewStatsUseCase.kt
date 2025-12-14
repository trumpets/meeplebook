package app.meeplebook.feature.overview.domain

import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.util.monthRangeFor
import app.meeplebook.feature.overview.OverviewStats
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case that calculates overview screen statistics from collection and plays data.
 * Uses optimized Room queries for better performance.
 */
class GetOverviewStatsUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val playsRepository: PlaysRepository
) {
    /**
     * Calculates statistics for the overview screen using optimized database queries.
     *
     * @return [OverviewStats] containing games count, play counts, and unplayed count
     */
    suspend operator fun invoke(): OverviewStats {
        // Use optimized Room queries instead of loading all data into memory
        val gamesCount = collectionRepository.getCollectionCount()
        val totalPlays = playsRepository.getTotalPlaysCount()

        // Calculate plays this month using Room query
        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val (start, end) = monthRangeFor(currentMonth)
        val playsThisMonth = playsRepository.getPlaysCountForMonth(start, end)

        // Use optimized Room query for unplayed games count
        val unplayedCount = collectionRepository.getUnplayedGamesCount()

        return OverviewStats(
            gamesCount = gamesCount,
            totalPlays = totalPlays,
            playsThisMonth = playsThisMonth,
            unplayedCount = unplayedCount
        )
    }
}