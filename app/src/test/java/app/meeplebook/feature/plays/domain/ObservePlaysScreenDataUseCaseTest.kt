package app.meeplebook.feature.plays.domain

import app.cash.turbine.test
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.PlayTestFactory.createPlay
import app.meeplebook.core.plays.domain.ObservePlayStatsUseCase
import app.meeplebook.core.plays.domain.ObservePlaysUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Unit tests for [ObservePlaysScreenDataUseCase].
 */
class ObservePlaysScreenDataUseCaseTest {

    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var observePlaysUseCase: ObservePlaysUseCase
    private lateinit var buildPlaysSectionsUseCase: BuildPlaysSectionsUseCase
    private lateinit var observePlayStatsUseCase: ObservePlayStatsUseCase
    private lateinit var useCase: ObservePlaysScreenDataUseCase

    // Fixed clock for deterministic testing - January 15, 2026
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-01-15T12:00:00Z"),
        ZoneOffset.UTC
    )

    @Before
    fun setUp() {
        fakePlaysRepository = FakePlaysRepository()
        observePlaysUseCase = ObservePlaysUseCase(fakePlaysRepository)
        buildPlaysSectionsUseCase = BuildPlaysSectionsUseCase()
        observePlayStatsUseCase = ObservePlayStatsUseCase(fakePlaysRepository, fixedClock)
        useCase = ObservePlaysScreenDataUseCase(
            observePlays = observePlaysUseCase,
            sectionBuilder = buildPlaysSectionsUseCase,
            observePlayStats = observePlayStatsUseCase
        )
    }

    @Test
    fun `invoke returns empty sections when no plays`() = runTest {
        // Given - empty repository (default state)

        // When
        val result = useCase().first()

        // Then
        assertTrue(result.sections.isEmpty())
        assertEquals(0L, result.stats.totalPlays)
        assertEquals(0L, result.stats.uniqueGamesCount)
        assertEquals(0L, result.stats.playsThisYear)
    }

    @Test
    fun `invoke organizes plays into sections by month and year`() = runTest {
        // Given - plays from different months
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan", date = Instant.parse("2026-01-15T20:00:00Z")),
            createPlay(localPlayId = 2, gameName = "Wingspan", date = Instant.parse("2026-01-10T19:00:00Z")),
            createPlay(localPlayId = 3, gameName = "Azul", date = Instant.parse("2025-12-20T18:00:00Z")),
            createPlay(localPlayId = 4, gameName = "Brass", date = Instant.parse("2025-12-15T17:00:00Z")),
            createPlay(localPlayId = 5, gameName = "Codenames", date = Instant.parse("2025-11-10T16:00:00Z"))
        )
        fakePlaysRepository.setPlays(plays)

        // When
        val result = useCase().first()

        // Then - 3 sections: Jan 2026, Dec 2025, Nov 2025
        assertEquals(3, result.sections.size)

        // Section 1: January 2026 (most recent first)
        val section1 = result.sections[0]
        assertEquals(2026, section1.monthYearDate.year)
        assertEquals(1, section1.monthYearDate.monthValue)
        assertEquals(2, section1.items.size)
        assertEquals("Catan", section1.items[0].gameName)
        assertEquals("Wingspan", section1.items[1].gameName)

        // Section 2: December 2025
        val section2 = result.sections[1]
        assertEquals(2025, section2.monthYearDate.year)
        assertEquals(12, section2.monthYearDate.monthValue)
        assertEquals(2, section2.items.size)
        assertEquals("Azul", section2.items[0].gameName)
        assertEquals("Brass", section2.items[1].gameName)

        // Section 3: November 2025
        val section3 = result.sections[2]
        assertEquals(2025, section3.monthYearDate.year)
        assertEquals(11, section3.monthYearDate.monthValue)
        assertEquals(1, section3.items.size)
        assertEquals("Codenames", section3.items[0].gameName)
    }

    @Test
    fun `invoke combines plays with stats correctly`() = runTest {
        // Given - 3 plays of 2 different games, 2 in current year
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan", gameId = 100, date = Instant.parse("2026-01-15T20:00:00Z")),
            createPlay(localPlayId = 2, gameName = "Wingspan", gameId = 200, date = Instant.parse("2026-01-10T19:00:00Z")),
            createPlay(localPlayId = 3, gameName = "Catan", gameId = 100, date = Instant.parse("2025-12-20T18:00:00Z"))
        )
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setPlaysCountForPeriod(2)

        // When
        val result = useCase().first()

        // Then - verify stats
        assertEquals(3L, result.stats.totalPlays)
        assertEquals(2L, result.stats.uniqueGamesCount)
        assertEquals(2L, result.stats.playsThisYear) // 2 plays in 2026
        assertEquals(2026, result.stats.currentYear)

        // Verify sections still correct
        assertEquals(2, result.sections.size)
    }

    @Test
    fun `invoke forwards query parameter to observePlays`() = runTest {
        // Given
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan"),
            createPlay(localPlayId = 2, gameName = "Wingspan")
        )
        fakePlaysRepository.setPlays(plays)

        // When
        useCase(query = "Catan").first()

        // Then
        assertEquals("Catan", fakePlaysRepository.lastObservePlaysQuery)
    }

    @Test
    fun `invoke with null query passes null to observePlays`() = runTest {
        // Given
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan")
        )
        fakePlaysRepository.setPlays(plays)

        // When
        useCase(query = null).first()

        // Then
        assertEquals(null, fakePlaysRepository.lastObservePlaysQuery)
    }

    @Test
    fun `invoke updates when plays change`() = runTest {
        // Given - initial state
        val initialPlays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan", date = Instant.parse("2026-01-15T20:00:00Z"))
        )
        fakePlaysRepository.setPlays(initialPlays)

        // When - observe the flow and update data while observing
        useCase().test {
            // Then - first emission
            val result1 = awaitItem()
            assertEquals(1, result1.sections.size)
            assertEquals(1, result1.sections[0].items.size)
            assertEquals("Catan", result1.sections[0].items[0].gameName)
            assertEquals(1L, result1.stats.totalPlays)

            // When - data changes (add a play)
            val updatedPlays = listOf(
                createPlay(localPlayId = 1, gameName = "Catan", date = Instant.parse("2026-01-15T20:00:00Z")),
                createPlay(localPlayId = 2, gameName = "Wingspan", gameId = 200, date = Instant.parse("2025-12-10T19:00:00Z"))
            )
            fakePlaysRepository.setPlays(updatedPlays)

            // Then - consume emissions until we reach the final stable state
            // setPlays() updates multiple StateFlows, which can cause intermediate emissions
            var result2: DomainPlaysScreenData
            do {
                result2 = awaitItem()
            } while (result2.sections.size != 2 || result2.stats.uniqueGamesCount != 2L)

            // Assert final stable state
            assertEquals(2, result2.sections.size) // Now 2 months
            assertEquals(1, result2.sections[0].items.size) // Jan 2026
            assertEquals(1, result2.sections[1].items.size) // Dec 2025
            assertEquals("Catan", result2.sections[0].items[0].gameName)
            assertEquals("Wingspan", result2.sections[1].items[0].gameName)
            assertEquals(2L, result2.stats.totalPlays)
            assertEquals(2L, result2.stats.uniqueGamesCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invoke updates when stats change independently`() = runTest {
        // Given - initial state
        fakePlaysRepository.setPlays(
            listOf(createPlay(localPlayId = 1, gameName = "Catan", date = Instant.parse("2026-01-15T20:00:00Z")))
        )
        fakePlaysRepository.setTotalPlaysCount(1L)
        fakePlaysRepository.setUniqueGamesCount(1L)
        fakePlaysRepository.setPlaysCountForPeriod(1L)

        // When - observe the flow and update stats
        useCase().test {
            // Then - first emission with initial stats
            val result1 = awaitItem()
            assertEquals(1L, result1.stats.totalPlays)
            assertEquals(1L, result1.stats.uniqueGamesCount)
            assertEquals(1L, result1.stats.playsThisYear)
            assertEquals(1, result1.sections.size)

            // When - total plays changes without changing plays list
            fakePlaysRepository.setTotalPlaysCount(5L)

            // Then - emission reflecting updated total plays only
            val result2 = awaitItem()
            assertEquals(5L, result2.stats.totalPlays)
            assertEquals(1L, result2.stats.uniqueGamesCount)
            assertEquals(1L, result2.stats.playsThisYear)
            // Sections remain the same
            assertEquals(1, result2.sections.size)

            // When - unique games count changes
            fakePlaysRepository.setUniqueGamesCount(3L)

            // Then - emission reflecting updated unique games count
            val result3 = awaitItem()
            assertEquals(5L, result3.stats.totalPlays)
            assertEquals(3L, result3.stats.uniqueGamesCount)
            assertEquals(1L, result3.stats.playsThisYear)
            // Sections remain the same
            assertEquals(1, result3.sections.size)

            // When - plays count for period changes
            fakePlaysRepository.setPlaysCountForPeriod(2L)

            // Then - emission reflecting updated period plays count
            val result4 = awaitItem()
            assertEquals(5L, result4.stats.totalPlays)
            assertEquals(3L, result4.stats.uniqueGamesCount)
            assertEquals(2L, result4.stats.playsThisYear)
            // Sections remain the same
            assertEquals(1, result4.sections.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invoke preserves play order within sections`() = runTest {
        // Given - multiple plays in same month in specific order
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "First", date = Instant.parse("2026-01-20T20:00:00Z")),
            createPlay(localPlayId = 2, gameName = "Second", date = Instant.parse("2026-01-15T19:00:00Z")),
            createPlay(localPlayId = 3, gameName = "Third", date = Instant.parse("2026-01-10T18:00:00Z"))
        )
        fakePlaysRepository.setPlays(plays)

        // When
        val result = useCase().first()

        // Then - order is preserved as returned by repository
        assertEquals(1, result.sections.size)
        assertEquals(3, result.sections[0].items.size)
        assertEquals("First", result.sections[0].items[0].gameName)
        assertEquals("Second", result.sections[0].items[1].gameName)
        assertEquals("Third", result.sections[0].items[2].gameName)
    }

    @Test
    fun `invoke handles plays spanning multiple years`() = runTest {
        // Given - plays from 3 different years
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Game2026", date = Instant.parse("2026-01-15T20:00:00Z")),
            createPlay(localPlayId = 2, gameName = "Game2025", date = Instant.parse("2025-06-15T19:00:00Z")),
            createPlay(localPlayId = 3, gameName = "Game2024", date = Instant.parse("2024-12-15T18:00:00Z"))
        )
        fakePlaysRepository.setPlays(plays)

        // When
        val result = useCase().first()

        // Then - 3 sections, one per month
        assertEquals(3, result.sections.size)
        // Most recent first
        assertEquals(2026, result.sections[0].monthYearDate.year)
        assertEquals(2025, result.sections[1].monthYearDate.year)
        assertEquals(2024, result.sections[2].monthYearDate.year)
    }
}
