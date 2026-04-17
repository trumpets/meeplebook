package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.util.yearRangeFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Instant
import java.time.Year
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Use case that observes play statistics summary.
 *
 * Combines multiple repository flows to provide aggregated statistics including:
 * - Total number of plays
 * - Number of unique games played
 * - Number of plays in the current year
 *
 * The statistics are automatically updated whenever the underlying play data changes.
 *
 * @property playsRepository Repository for accessing play data.
 * @property clock Clock instance for determining the current year (injectable for testing).
 */
class ObservePlayStatsUseCase @Inject constructor(
    private val playsRepository: PlaysRepository,
    private val clock: Clock
) {
    /**
     * Observes play statistics summary as a continuous flow.
     *
     * @return Flow emitting [DomainPlayStatsSummary] whenever play data changes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<DomainPlayStatsSummary> {

        val now = Instant.now(clock)
        val year = Year.from(
            now.atZone(ZoneOffset.UTC)
        )

        val range = yearRangeFor(year, ZoneOffset.UTC)

        return combine(
            playsRepository.observeTotalPlaysCount(),
            playsRepository.observeUniqueGamesCount(),
            playsRepository.observePlaysCountForPeriod(range.start, range.end)
        ) { total, unique, thisYear ->
            DomainPlayStatsSummary(
                totalPlays = total,
                uniqueGamesCount = unique,
                playsThisYear = thisYear,
                currentYear = year.value
            )
        }
    }
}