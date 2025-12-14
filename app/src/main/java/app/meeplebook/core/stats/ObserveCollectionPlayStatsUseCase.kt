package app.meeplebook.core.stats

import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.util.Range
import app.meeplebook.core.util.monthRangeFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Observes collection and play statistics for the overview screen.
 *
 * Emits statistics including total games owned, total plays recorded,
 * plays this month, and count of unplayed games. Updates whenever the
 * underlying collection or plays data changes in the database.
 */
class ObserveCollectionPlayStatsUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val playsRepository: PlaysRepository,
    private val clock: Clock
) {
    operator fun invoke(): Flow<CollectionPlayStats> {
        val range = currentMonthRange(clock)

        return combine(
            collectionRepository.observeCollectionCount(),
            playsRepository.observeTotalPlaysCount(),
            playsRepository.observePlaysCountForPeriod(range.start, range.end),
            collectionRepository.observeUnplayedGamesCount()
        ) { games, totalPlays, playsThisMonth, unplayed ->
            CollectionPlayStats(
                gamesCount = games,
                totalPlays = totalPlays,
                playsInPeriod = playsThisMonth,
                unplayedCount = unplayed
            )
        }
    }

    private fun currentMonthRange(clock: Clock): Range {
        val now = Instant.now(clock)
        val currentYearMonth = YearMonth.from(
            now.atZone(ZoneOffset.UTC)
        )

        return monthRangeFor(currentYearMonth, ZoneOffset.UTC)
    }
}