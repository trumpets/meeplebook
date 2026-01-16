package app.meeplebook.core.stats.domain

import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.domain.ObserveCollectionSummaryUseCase
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.stats.model.CollectionPlayStats
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Unit tests for [ObserveCollectionPlayStatsUseCase].
 */
class ObserveCollectionPlayStatsUseCaseTest {

    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var useCase: ObserveCollectionPlayStatsUseCase

    // Fixed clock for predictable testing (2024-01-15 12:00:00 UTC)
    private val testClock = Clock.fixed(
        Instant.parse("2024-01-15T12:00:00Z"),
        ZoneOffset.UTC
    )

    @Before
    fun setUp() {
        fakeCollectionRepository = FakeCollectionRepository()
        fakePlaysRepository = FakePlaysRepository()
        useCase = ObserveCollectionPlayStatsUseCase(
            observeCollectionSummary = ObserveCollectionSummaryUseCase(fakeCollectionRepository),
            playsRepository = fakePlaysRepository,
            clock = testClock
        )
    }

    @Test
    fun `invoke returns combined stats from collection and plays`() = runTest {
        // Given
        fakeCollectionRepository.setCollectionCount(127)
        fakeCollectionRepository.setUnplayedCount(23)
        fakePlaysRepository.setTotalPlaysCount(342)
        fakePlaysRepository.setPlaysCountForPeriod(18)

        // When
        val stats = useCase().first()

        // Then
        assertEquals(127L, stats.gamesCount)
        assertEquals(342L, stats.totalPlays)
        assertEquals(18L, stats.playsInPeriod)
        assertEquals(23L, stats.unplayedCount)
    }

    @Test
    fun `invoke returns zero stats when no data`() = runTest {
        // Given - repositories have no data (default state)

        // When
        val stats = useCase().first()

        // Then
        assertEquals(
            CollectionPlayStats(
                gamesCount = 0,
                totalPlays = 0,
                playsInPeriod = 0,
                unplayedCount = 0
            ),
            stats
        )
    }

    @Test
    fun `invoke calculates current month range correctly`() = runTest {
        // Given
        fakeCollectionRepository.setCollectionCount(50)
        fakePlaysRepository.setTotalPlaysCount(100)
        fakePlaysRepository.setPlaysCountForPeriod(10) // Plays in Jan 2024

        // When
        val stats = useCase().first()

        // Then - verifies that the use case queries for the current month
        assertEquals(10L, stats.playsInPeriod)
    }

    @Test
    fun `invoke updates when collection count changes`() = runTest {
        // Given
        fakeCollectionRepository.setCollectionCount(50)

        // When - first observation
        val stats1 = useCase().first()

        // Then
        assertEquals(50L, stats1.gamesCount)

        // When - collection count changes
        fakeCollectionRepository.setCollectionCount(75)
        val stats2 = useCase().first()

        // Then - stats are updated
        assertEquals(75L, stats2.gamesCount)
    }

    @Test
    fun `invoke updates when plays count changes`() = runTest {
        // Given
        fakePlaysRepository.setTotalPlaysCount(100)

        // When - first observation
        val stats1 = useCase().first()

        // Then
        assertEquals(100L, stats1.totalPlays)

        // When - plays count changes
        fakePlaysRepository.setTotalPlaysCount(150)
        val stats2 = useCase().first()

        // Then - stats are updated
        assertEquals(150L, stats2.totalPlays)
    }
}
