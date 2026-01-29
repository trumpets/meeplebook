package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.FakePlaysRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Unit tests for [ObservePlayStatsUseCase].
 */
class ObservePlayStatsUseCaseTest {

    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var useCase: ObservePlayStatsUseCase

    // Fixed clock for predictable testing (2024-06-15 12:00:00 UTC)
    private val testClock = Clock.fixed(
        Instant.parse("2024-06-15T12:00:00Z"),
        ZoneOffset.UTC
    )

    @Before
    fun setUp() {
        fakePlaysRepository = FakePlaysRepository()
        useCase = ObservePlayStatsUseCase(
            playsRepository = fakePlaysRepository,
            clock = testClock
        )
    }

    @Test
    fun `invoke returns combined play statistics`() = runTest {
        // Given
        fakePlaysRepository.setTotalPlaysCount(342)
        fakePlaysRepository.setUniqueGamesCount(87)
        fakePlaysRepository.setPlaysCountForPeriod(52)

        // When
        val stats = useCase().first()

        // Then
        assertEquals(342L, stats.totalPlays)
        assertEquals(87L, stats.uniqueGamesCount)
        assertEquals(52L, stats.playsThisYear)
        assertEquals(2024, stats.currentYear)
    }

    @Test
    fun `invoke returns zero stats when no data`() = runTest {
        // Given - repositories have no data (default state)

        // When
        val stats = useCase().first()

        // Then
        assertEquals(
            DomainPlayStatsSummary(
                totalPlays = 0,
                uniqueGamesCount = 0,
                playsThisYear = 0,
                currentYear = 2024
            ),
            stats
        )
    }

    @Test
    fun `invoke calculates current year correctly`() = runTest {
        // Given
        fakePlaysRepository.setTotalPlaysCount(100)
        fakePlaysRepository.setUniqueGamesCount(25)
        fakePlaysRepository.setPlaysCountForPeriod(15) // Plays in 2024

        // When
        val stats = useCase().first()

        // Then - verifies that the current year is derived from the clock and stats are mapped correctly
        assertEquals(2024, stats.currentYear)
        assertEquals(15L, stats.playsThisYear)
    }

    @Test
    fun `invoke updates when total plays count changes`() = runTest {
        // Given
        fakePlaysRepository.setTotalPlaysCount(100)
        fakePlaysRepository.setUniqueGamesCount(25)
        fakePlaysRepository.setPlaysCountForPeriod(10)

        // When - first observation
        val stats1 = useCase().first()

        // Then
        assertEquals(100L, stats1.totalPlays)

        // When - total plays count changes
        fakePlaysRepository.setTotalPlaysCount(150)
        val stats2 = useCase().first()

        // Then - stats are updated
        assertEquals(150L, stats2.totalPlays)
    }

    @Test
    fun `invoke updates when unique games count changes`() = runTest {
        // Given
        fakePlaysRepository.setTotalPlaysCount(100)
        fakePlaysRepository.setUniqueGamesCount(25)
        fakePlaysRepository.setPlaysCountForPeriod(10)

        // When - first observation
        val stats1 = useCase().first()

        // Then
        assertEquals(25L, stats1.uniqueGamesCount)

        // When - unique games count changes
        fakePlaysRepository.setUniqueGamesCount(40)
        val stats2 = useCase().first()

        // Then - stats are updated
        assertEquals(40L, stats2.uniqueGamesCount)
    }

    @Test
    fun `invoke updates when plays this year changes`() = runTest {
        // Given
        fakePlaysRepository.setTotalPlaysCount(100)
        fakePlaysRepository.setUniqueGamesCount(25)
        fakePlaysRepository.setPlaysCountForPeriod(10)

        // When - first observation
        val stats1 = useCase().first()

        // Then
        assertEquals(10L, stats1.playsThisYear)

        // When - plays for the year change
        fakePlaysRepository.setPlaysCountForPeriod(20)
        val stats2 = useCase().first()

        // Then - stats are updated
        assertEquals(20L, stats2.playsThisYear)
    }

    @Test
    fun `invoke with different clock returns correct year`() = runTest {
        // Given - clock set to 2025
        val clock2025 = Clock.fixed(
            Instant.parse("2025-03-20T10:00:00Z"),
            ZoneOffset.UTC
        )
        val useCase2025 = ObservePlayStatsUseCase(
            playsRepository = fakePlaysRepository,
            clock = clock2025
        )
        fakePlaysRepository.setTotalPlaysCount(50)
        fakePlaysRepository.setUniqueGamesCount(10)
        fakePlaysRepository.setPlaysCountForPeriod(5)

        // When
        val stats = useCase2025().first()

        // Then
        assertEquals(2025, stats.currentYear)
        assertEquals(5L, stats.playsThisYear)
    }

    @Test
    fun `invoke combines all three statistics correctly`() = runTest {
        // Given - different values for each statistic
        fakePlaysRepository.setTotalPlaysCount(500)
        fakePlaysRepository.setUniqueGamesCount(120)
        fakePlaysRepository.setPlaysCountForPeriod(75)

        // When
        val stats = useCase().first()

        // Then - all values are present and correct
        assertEquals(500L, stats.totalPlays)
        assertEquals(120L, stats.uniqueGamesCount)
        assertEquals(75L, stats.playsThisYear)
        assertEquals(2024, stats.currentYear)
    }
}
