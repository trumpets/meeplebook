package app.meeplebook.core.plays

import app.meeplebook.core.plays.PlayTestFactory.createPlay
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [FakePlaysRepository].
 * Tests the fake implementation's behavior, particularly the automatic computation
 * of derived values like unique games count.
 */
class FakePlaysRepositoryTest {

    private lateinit var repository: FakePlaysRepository

    @Before
    fun setUp() {
        repository = FakePlaysRepository()
    }

    // --- observeUniqueGamesCount tests ---

    @Test
    fun `observeUniqueGamesCount returns zero initially`() = runTest {
        val result = repository.observeUniqueGamesCount().first()

        assertEquals(0L, result)
    }

    @Test
    fun `observeUniqueGamesCount can be set manually`() = runTest {
        repository.setUniqueGamesCount(5L)

        val result = repository.observeUniqueGamesCount().first()

        assertEquals(5L, result)
    }

    @Test
    fun `setPlays automatically computes unique games count for single game`() = runTest {
        val plays = listOf(
            createPlay(id = 1, gameName = "Catan", gameId = 100),
            createPlay(id = 2, gameName = "Catan", gameId = 100),
            createPlay(id = 3, gameName = "Catan", gameId = 100)
        )

        repository.setPlays(plays)
        val result = repository.observeUniqueGamesCount().first()

        assertEquals(1L, result)
    }

    @Test
    fun `setPlays automatically computes unique games count for multiple games`() = runTest {
        val plays = listOf(
            createPlay(id = 1, gameName = "Catan", gameId = 100),
            createPlay(id = 2, gameName = "Wingspan", gameId = 200),
            createPlay(id = 3, gameName = "Azul", gameId = 300),
            createPlay(id = 4, gameName = "Catan", gameId = 100)
        )

        repository.setPlays(plays)
        val result = repository.observeUniqueGamesCount().first()

        assertEquals(3L, result)
    }

    @Test
    fun `setPlays with empty list sets unique games count to zero`() = runTest {
        repository.setUniqueGamesCount(5L)

        repository.setPlays(emptyList())
        val result = repository.observeUniqueGamesCount().first()

        assertEquals(0L, result)
    }

    @Test
    fun `syncPlays success automatically computes unique games count`() = runTest {
        val plays = listOf(
            createPlay(id = 1, gameName = "Game 1", gameId = 100),
            createPlay(id = 2, gameName = "Game 2", gameId = 200),
            createPlay(id = 3, gameName = "Game 1", gameId = 100)
        )
        repository.syncPlaysResult = AppResult.Success(plays)

        repository.syncPlays("testuser")
        val result = repository.observeUniqueGamesCount().first()

        assertEquals(2L, result)
    }

    @Test
    fun `syncPlays failure does not change unique games count`() = runTest {
        repository.setUniqueGamesCount(3L)
        repository.syncPlaysResult = AppResult.Failure(PlayError.NetworkError)

        repository.syncPlays("testuser")
        val result = repository.observeUniqueGamesCount().first()

        assertEquals(3L, result)
    }

    @Test
    fun `clearPlays resets unique games count to zero`() = runTest {
        repository.setUniqueGamesCount(10L)

        repository.clearPlays()
        val result = repository.observeUniqueGamesCount().first()

        assertEquals(0L, result)
    }

    @Test
    fun `unique games count updates correctly when plays change`() = runTest {
        // Initially one game
        val initialPlays = listOf(
            createPlay(id = 1, gameName = "Catan", gameId = 100)
        )
        repository.setPlays(initialPlays)
        val result1 = repository.observeUniqueGamesCount().first()
        assertEquals(1L, result1)

        // Add more plays with different games
        val updatedPlays = listOf(
            createPlay(id = 1, gameName = "Catan", gameId = 100),
            createPlay(id = 2, gameName = "Wingspan", gameId = 200),
            createPlay(id = 3, gameName = "Azul", gameId = 300)
        )
        repository.setPlays(updatedPlays)
        val result2 = repository.observeUniqueGamesCount().first()
        assertEquals(3L, result2)

        // Clear all plays
        repository.clearPlays()
        val result3 = repository.observeUniqueGamesCount().first()
        assertEquals(0L, result3)
    }

    // --- Test interaction with other computed values ---

    @Test
    fun `setPlays updates all computed values consistently`() = runTest {
        val plays = listOf(
            createPlay(
                id = 1,
                gameName = "Catan",
                gameId = 100,
                quantity = 2,
                date = Instant.parse("2024-01-15T20:00:00Z")
            ),
            createPlay(
                id = 2,
                gameName = "Wingspan",
                gameId = 200,
                quantity = 1,
                date = Instant.parse("2024-01-14T19:00:00Z")
            ),
            createPlay(
                id = 3,
                gameName = "Catan",
                gameId = 100,
                quantity = 1,
                date = Instant.parse("2024-01-13T18:00:00Z")
            )
        )

        repository.setPlays(plays)

        // Check unique games count
        val uniqueGamesCount = repository.observeUniqueGamesCount().first()
        assertEquals(2L, uniqueGamesCount)

        // Check total plays count (sum of quantities)
        val totalPlaysCount = repository.observeTotalPlaysCount().first()
        assertEquals(4L, totalPlaysCount)

        // Check recent plays (sorted by date, limited to 5)
        val recentPlays = repository.observeRecentPlays(5).first()
        assertEquals(3, recentPlays.size)
        assertEquals("Catan", recentPlays[0].gameName)
        assertEquals(Instant.parse("2024-01-15T20:00:00Z"), recentPlays[0].date)
    }

    @Test
    fun `clearPlays resets all computed values`() = runTest {
        val plays = listOf(
            createPlay(id = 1, gameName = "Game 1", gameId = 100, quantity = 2),
            createPlay(id = 2, gameName = "Game 2", gameId = 200, quantity = 3)
        )
        repository.setPlays(plays)

        repository.clearPlays()

        assertEquals(0L, repository.observeUniqueGamesCount().first())
        assertEquals(0L, repository.observeTotalPlaysCount().first())
        assertEquals(0, repository.observeRecentPlays(5).first().size)
        assertEquals(0, repository.observePlays().first().size)
    }
}
