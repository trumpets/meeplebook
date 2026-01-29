package app.meeplebook.core.plays.local

import app.meeplebook.core.plays.PlayTestFactory.createPlay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [FakePlaysLocalDataSource].
 * Tests the fake implementation's behavior, particularly the manual control
 * of computed values via setter methods.
 */
class FakePlaysLocalDataSourceTest {

    private lateinit var dataSource: FakePlaysLocalDataSource

    @Before
    fun setUp() {
        dataSource = FakePlaysLocalDataSource()
    }

    // --- observeUniqueGamesCount tests ---

    @Test
    fun `observeUniqueGamesCount returns zero initially`() = runTest {
        val result = dataSource.observeUniqueGamesCount().first()

        assertEquals(0L, result)
    }

    @Test
    fun `setUniqueGamesCount updates the observable value`() = runTest {
        dataSource.setUniqueGamesCount(7L)

        val result = dataSource.observeUniqueGamesCount().first()

        assertEquals(7L, result)
    }

    @Test
    fun `setUniqueGamesCount can be called multiple times`() = runTest {
        dataSource.setUniqueGamesCount(3L)
        val result1 = dataSource.observeUniqueGamesCount().first()
        assertEquals(3L, result1)

        dataSource.setUniqueGamesCount(10L)
        val result2 = dataSource.observeUniqueGamesCount().first()
        assertEquals(10L, result2)

        dataSource.setUniqueGamesCount(0L)
        val result3 = dataSource.observeUniqueGamesCount().first()
        assertEquals(0L, result3)
    }

    @Test
    fun `clearPlays resets unique games count to zero`() = runTest {
        dataSource.setUniqueGamesCount(15L)

        dataSource.clearPlays()
        val result = dataSource.observeUniqueGamesCount().first()

        assertEquals(0L, result)
    }

    @Test
    fun `savePlays does not automatically update unique games count`() = runTest {
        val plays = listOf(
            createPlay(id = 1, gameName = "Catan", gameId = 100),
            createPlay(id = 2, gameName = "Wingspan", gameId = 200)
        )

        dataSource.savePlays(plays)
        val result = dataSource.observeUniqueGamesCount().first()

        // Unique games count remains at default value since it must be set manually
        assertEquals(0L, result)
    }

    // --- clearPlays resets all state tests ---

    @Test
    fun `clearPlays resets all computed values to initial state`() = runTest {
        // Set up various state values
        val plays = listOf(
            createPlay(id = 1, gameName = "Game 1", gameId = 100)
        )
        dataSource.savePlays(plays)
        dataSource.setTotalPlaysCount(10L)
        dataSource.setUniqueGamesCount(5L)
        dataSource.setPlaysCountForMonth(
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-02-01T00:00:00Z"),
            3L
        )
        dataSource.setRecentPlays(5, plays)

        // Clear everything
        dataSource.clearPlays()

        // Verify all state is reset
        assertEquals(0, dataSource.observePlays().first().size)
        assertEquals(0L, dataSource.observeTotalPlaysCount().first())
        assertEquals(0L, dataSource.observeUniqueGamesCount().first())
        assertEquals(
            0L,
            dataSource.observePlaysCountForMonth(
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-02-01T00:00:00Z")
            ).first()
        )
        assertEquals(0, dataSource.observeRecentPlays(5).first().size)
    }

    @Test
    fun `clearPlays allows fresh state setup`() = runTest {
        // Set up initial state
        dataSource.setUniqueGamesCount(10L)
        dataSource.setTotalPlaysCount(20L)

        // Clear and verify
        dataSource.clearPlays()
        assertEquals(0L, dataSource.observeUniqueGamesCount().first())

        // Set up fresh state
        dataSource.setUniqueGamesCount(3L)
        val result = dataSource.observeUniqueGamesCount().first()
        assertEquals(3L, result)
    }

    // --- Test manual control pattern ---

    @Test
    fun `manual setters provide independent control of computed values`() = runTest {
        // Save some plays
        val plays = listOf(
            createPlay(id = 1, gameName = "Catan", gameId = 100, quantity = 2),
            createPlay(id = 2, gameName = "Wingspan", gameId = 200, quantity = 1),
            createPlay(id = 3, gameName = "Catan", gameId = 100, quantity = 1)
        )
        dataSource.savePlays(plays)

        // Manually set computed values to specific test values
        dataSource.setTotalPlaysCount(100L)
        dataSource.setUniqueGamesCount(50L)
        dataSource.setRecentPlays(5, listOf(plays[0]))

        // Verify each can be controlled independently
        assertEquals(100L, dataSource.observeTotalPlaysCount().first())
        assertEquals(50L, dataSource.observeUniqueGamesCount().first())
        assertEquals(1, dataSource.observeRecentPlays(5).first().size)
        assertEquals(3, dataSource.observePlays().first().size)
    }

    // --- observePlays and related methods work correctly ---

    @Test
    fun `observePlays returns saved plays`() = runTest {
        val plays = listOf(
            createPlay(id = 1, gameName = "Game 1", gameId = 100)
        )

        dataSource.savePlays(plays)
        val result = dataSource.observePlays().first()

        assertEquals(plays, result)
    }

    @Test
    fun `savePlays merges plays correctly`() = runTest {
        val initialPlays = listOf(
            createPlay(id = 1, gameName = "Game 1", gameId = 100)
        )
        dataSource.savePlays(initialPlays)

        val newPlays = listOf(
            createPlay(id = 1, gameName = "Game 1 Updated", gameId = 100),
            createPlay(id = 2, gameName = "Game 2", gameId = 200)
        )
        dataSource.savePlays(newPlays)

        val result = dataSource.observePlays().first()
        assertEquals(2, result.size)
        assertEquals("Game 1 Updated", result.find { it.id == 1L }?.gameName)
        assertEquals("Game 2", result.find { it.id == 2L }?.gameName)
    }
}
