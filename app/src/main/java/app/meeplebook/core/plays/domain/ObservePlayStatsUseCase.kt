package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.util.yearRangeFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.time.delay
import java.time.Clock
import java.time.Duration
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

        return observeCurrentYear().flatMapLatest { year ->
            val range = yearRangeFor(year, ZoneOffset.UTC)

            combine(
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

    /**
     * Observes the current year.
     *
     * Emits the current year immediately, then suspends until the start of the next year
     * before emitting again. This ensures that the current year is accurate even if the app
     * remains open across a year boundary.
     */
    private fun observeCurrentYear(): Flow<Year> =
        flow {
            while (true) {
                val now = Instant.now(clock)
                val currentYear = Year.from(
                    now.atZone(ZoneOffset.UTC)
                )

                emit(currentYear)

                val nextYearInstant = currentYear
                    .plusYears(1)
                    .atDay(1)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()

                delay(Duration.between(now, nextYearInstant))
            }
        }.distinctUntilChanged()
}