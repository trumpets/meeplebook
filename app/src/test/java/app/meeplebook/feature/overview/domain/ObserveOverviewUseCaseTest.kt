package app.meeplebook.feature.overview.domain

import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.domain.ObserveCollectionHighlightsUseCase
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.PlayTestFactory.createPlay
import app.meeplebook.core.plays.domain.ObserveRecentPlaysUseCase
import app.meeplebook.core.stats.domain.ObserveCollectionPlayStatsUseCase
import app.meeplebook.core.sync.FakeSyncTimeRepository
import app.meeplebook.core.sync.domain.ObserveLastFullSyncUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Unit tests for [ObserveOverviewUseCase].
 */
class ObserveOverviewUseCaseTest {

    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var useCase: ObserveOverviewUseCase

    private val testClock = Clock.fixed(
        Instant.parse("2024-01-15T12:00:00Z"),
        ZoneOffset.UTC
    )

    @Before
    fun setUp() {
        fakeCollectionRepository = FakeCollectionRepository()
        fakePlaysRepository = FakePlaysRepository()
        fakeSyncTimeRepository = FakeSyncTimeRepository()

        val observeStats = ObserveCollectionPlayStatsUseCase(
            collectionRepository = fakeCollectionRepository,
            playsRepository = fakePlaysRepository,
            clock = testClock
        )
        val observeRecentPlays = ObserveRecentPlaysUseCase(fakePlaysRepository)
        val observeHighlights = ObserveCollectionHighlightsUseCase(fakeCollectionRepository)
        val observeLastSync = ObserveLastFullSyncUseCase(fakeSyncTimeRepository)

        useCase = ObserveOverviewUseCase(
            observeStats = observeStats,
            observeRecentPlays = observeRecentPlays,
            observeHighlights = observeHighlights,
            observeLastSync = observeLastSync
        )
    }

    @Test
    fun `invoke returns complete overview with all data`() = runTest {
        // Given
        fakeCollectionRepository.setCollectionCount(50)
        fakeCollectionRepository.setUnplayedCount(10)
        fakePlaysRepository.setTotalPlaysCount(100)
        fakePlaysRepository.setPlaysCountForPeriod(15)

        val recentlyAdded = CollectionItem(
            gameId = 1,
            subtype = GameSubtype.BOARDGAME,
            name = "Azul",
            yearPublished = 2017,
            thumbnail = null,
            lastModifiedDate = Instant.parse("2024-01-10T10:00:00Z")
        )
        val suggested = CollectionItem(
            gameId = 2,
            subtype = GameSubtype.BOARDGAME,
            name = "Wingspan",
            yearPublished = 2019,
            thumbnail = null,
            lastModifiedDate = Instant.parse("2024-01-01T10:00:00Z")
        )
        fakeCollectionRepository.setMostRecentlyAdded(recentlyAdded)
        fakeCollectionRepository.setFirstUnplayed(suggested)

        val recentPlays = listOf(
            createPlay(id = 1, gameName = "Catan"),
            createPlay(id = 2, gameName = "Ticket to Ride")
        )
        fakePlaysRepository.setRecentPlays(recentPlays)

        val syncTime = Instant.parse("2024-01-15T10:00:00Z")
        fakeSyncTimeRepository.updateFullSyncTime(syncTime)

        // When
        val overview = useCase().first()

        // Then
        assertEquals(50L, overview.stats.gamesCount)
        assertEquals(100L, overview.stats.totalPlays)
        assertEquals(15L, overview.stats.playsInPeriod)
        assertEquals(10L, overview.stats.unplayedCount)

        assertEquals(2, overview.recentPlays.size)
        assertEquals("Catan", overview.recentPlays[0].gameName)

        assertNotNull(overview.recentlyAddedGame)
        assertEquals("Azul", overview.recentlyAddedGame?.gameName)

        assertNotNull(overview.suggestedGame)
        assertEquals("Wingspan", overview.suggestedGame?.gameName)

        assertEquals(syncTime, overview.lastSyncedDate)
    }

    @Test
    fun `invoke returns overview with empty data`() = runTest {
        // Given - no data in repositories

        // When
        val overview = useCase().first()

        // Then
        assertEquals(0L, overview.stats.gamesCount)
        assertEquals(0L, overview.stats.totalPlays)
        assertEquals(0L, overview.stats.playsInPeriod)
        assertEquals(0L, overview.stats.unplayedCount)

        assertEquals(0, overview.recentPlays.size)
        assertNull(overview.recentlyAddedGame)
        assertNull(overview.suggestedGame)
        assertNull(overview.lastSyncedDate)
    }

    @Test
    fun `invoke returns overview with stats but no highlights`() = runTest {
        // Given
        fakeCollectionRepository.setCollectionCount(25)
        fakePlaysRepository.setTotalPlaysCount(50)

        // When
        val overview = useCase().first()

        // Then
        assertEquals(25L, overview.stats.gamesCount)
        assertEquals(50L, overview.stats.totalPlays)
        assertNull(overview.recentlyAddedGame)
        assertNull(overview.suggestedGame)
    }

    @Test
    fun `invoke returns overview with stats and plays but no sync time`() = runTest {
        // Given
        fakeCollectionRepository.setCollectionCount(30)
        fakePlaysRepository.setTotalPlaysCount(75)

        val recentPlays = listOf(createPlay(id = 1, gameName = "Catan"))
        fakePlaysRepository.setRecentPlays(recentPlays)

        // When
        val overview = useCase().first()

        // Then
        assertEquals(30L, overview.stats.gamesCount)
        assertEquals(1, overview.recentPlays.size)
        assertNull(overview.lastSyncedDate)
    }

    @Test
    fun `invoke updates when any data source changes`() = runTest {
        // Given - initial state
        fakeCollectionRepository.setCollectionCount(10)

        // When - first observation
        val overview1 = useCase().first()

        // Then
        assertEquals(10L, overview1.stats.gamesCount)

        // When - data changes
        fakeCollectionRepository.setCollectionCount(20)
        val overview2 = useCase().first()

        // Then - overview is updated
        assertEquals(20L, overview2.stats.gamesCount)
    }
}
