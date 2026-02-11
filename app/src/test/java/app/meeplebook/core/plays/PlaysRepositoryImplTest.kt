package app.meeplebook.core.plays

import app.meeplebook.core.network.RetryException
import app.meeplebook.core.plays.PlayTestFactory.createPlay
import app.meeplebook.core.plays.PlayTestFactory.createRemotePlayDto
import app.meeplebook.core.plays.domain.CreatePlayCommand
import app.meeplebook.core.plays.domain.CreatePlayerCommand
import app.meeplebook.core.plays.local.FakePlaysLocalDataSource
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.plays.remote.FakePlaysRemoteDataSource
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.util.parseDateString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant

class PlaysRepositoryImplTest {

    private lateinit var local: FakePlaysLocalDataSource
    private lateinit var remote: FakePlaysRemoteDataSource
    private lateinit var repository: PlaysRepositoryImpl

    private val testPlay = createPlay(
        localPlayId = 1,
        gameName = "Test Game",
        date = parseDateString("2024-01-01"),
        location = "Home",
        gameId = 123
    )

    private val testCreatePlayCommand = createPlayCommand(
        date = parseDateString("2024-01-01"),
        location = "Home",
        gameId = 123,
        gameName = "Test Game",
        players = emptyList()
    )

    @Before
    fun setup() {
        local = FakePlaysLocalDataSource()
        remote = FakePlaysRemoteDataSource()
        repository = PlaysRepositoryImpl(local, remote)
    }

    @Test
    fun `observePlays returns flow from local data source`() = runTest {
        val plays = listOf(testPlay)
        local.setPlays(plays)

        val result = repository.observePlays().first()

        assertEquals(plays, result)
    }

    @Test
    fun `observePlays with null query returns all plays`() = runTest {
        val plays = listOf(testPlay)
        local.setPlays(plays)

        val result = repository.observePlays(null).first()

        assertEquals(plays, result)
    }

    @Test
    fun `observePlays with empty query returns all plays`() = runTest {
        val plays = listOf(testPlay)
        local.setPlays(plays)

        val result = repository.observePlays("").first()

        assertEquals(plays, result)
    }

    @Test
    fun `observePlays with blank query returns all plays`() = runTest {
        val plays = listOf(testPlay)
        local.setPlays(plays)

        val result = repository.observePlays("   ").first()

        assertEquals(plays, result)
    }

    @Test
    fun `observePlays with non-blank query filters by game name`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan", location = "Home"),
            createPlay(localPlayId = 2, gameName = "Azul", location = "Home"),
            createPlay(localPlayId = 3, gameName = "Carcassonne", location = "Home")
        )
        local.setPlays(plays)

        val result = repository.observePlays("Catan").first()

        assertEquals(1, result.size)
        assertEquals("Catan", result[0].gameName)
    }

    @Test
    fun `observePlays with non-blank query filters by location`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan", location = "Home"),
            createPlay(localPlayId = 2, gameName = "Azul", location = "Game Store"),
            createPlay(localPlayId = 3, gameName = "Carcassonne", location = "Friend's House")
        )
        local.setPlays(plays)

        val result = repository.observePlays("Store").first()

        assertEquals(1, result.size)
        assertEquals("Game Store", result[0].location)
    }

    @Test
    fun `observePlays with non-blank query filters case-insensitively`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan", location = "Home"),
            createPlay(localPlayId = 2, gameName = "Azul", location = "Home")
        )
        local.setPlays(plays)

        val result = repository.observePlays("catan").first()

        assertEquals(1, result.size)
        assertEquals("Catan", result[0].gameName)
    }

    @Test
    fun `observePlays with non-blank query trims whitespace`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan", location = "Home"),
            createPlay(localPlayId = 2, gameName = "Azul", location = "Home")
        )
        local.setPlays(plays)

        val result = repository.observePlays("  Catan  ").first()

        assertEquals(1, result.size)
        assertEquals("Catan", result[0].gameName)
    }

    @Test
    fun `observePlays with non-blank query returns empty list when no matches`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan", location = "Home")
        )
        local.setPlays(plays)

        val result = repository.observePlays("Monopoly").first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getPlays returns data from local data source`() = runTest {
        val plays = listOf(testPlay)
        local.setPlays(plays)

        val result = repository.getPlays()

        assertEquals(plays, result)
    }

    @Test
    fun `syncPlays success fetches from remote and saves to local`() = runTest {
        val remotePlays = listOf(
            createRemotePlayDto(
                remoteId = 1,
                gameName = "Test Game",
                date = parseDateString("2024-01-01"),
                location = "Home",
                gameId = 123
            )
        )
        remote.playsToReturn = remotePlays

        val result = repository.syncPlays("user123")

        assertTrue(result is AppResult.Success)
        assertTrue(remote.fetchPlaysCalled)
        assertEquals("user123", remote.lastFetchUsername)
        assertEquals(1, remote.lastFetchPage)
        
        val savedPlays = local.getPlays()
        assertEquals(1, savedPlays.size)
        assertEquals("Test Game", savedPlays[0].gameName)
        assertEquals(123L, savedPlays[0].gameId)
        
        // Verify PlayId.Remote structure and sync status
        val playId = savedPlays[0].playId
        assertTrue(playId is PlayId.Remote)
        assertEquals(1L, (playId as PlayId.Remote).remoteId)
        assertEquals(PlaySyncStatus.SYNCED, savedPlays[0].syncStatus)
    }

    @Test
    fun `syncPlays fetches multiple pages`() = runTest {
        // Simulate multi-page response
        val page1Plays = List(100) { i ->
            createRemotePlayDto(remoteId = i + 1L, gameName = "Game ${i + 1}")
        }
        val page2Plays = List(50) { i ->
            createRemotePlayDto(remoteId = i + 101L, gameName = "Game ${i + 101}")
        }

        // Configure fake to return different results per page
        remote.playsToReturnByPage = mapOf(
            1 to page1Plays,
            2 to page2Plays
        )

        val result = repository.syncPlays("user123")

        assertTrue(result is AppResult.Success)
        val allPlays = repository.getPlays()
        assertEquals(150, allPlays.size)
        assertEquals(150, local.getPlays().size)
    }

    @Test
    fun `syncPlays returns NotLoggedIn on IllegalArgumentException`() = runTest {
        remote.shouldThrowException = IllegalArgumentException("Invalid username")

        val result = repository.syncPlays("")

        assertTrue(result is AppResult.Failure)
        assertEquals(PlayError.NotLoggedIn, (result as AppResult.Failure).error)
        assertTrue(local.getPlays().isEmpty())
    }

    @Test
    fun `syncPlays returns NetworkError on IOException`() = runTest {
        remote.shouldThrowException = IOException("Network error")

        val result = repository.syncPlays("user123")

        assertTrue(result is AppResult.Failure)
        assertEquals(PlayError.NetworkError, (result as AppResult.Failure).error)
    }

    @Test
    fun `syncPlays returns MaxRetriesExceeded on RetryException`() = runTest {
        val retryException = RetryException("Retry failed", "user123", 202, 5, 1000L)
        remote.shouldThrowException = retryException

        val result = repository.syncPlays("user123")

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is PlayError.MaxRetriesExceeded)
        assertEquals(retryException, (error as PlayError.MaxRetriesExceeded).exception)
    }

    @Test
    fun `syncPlays returns Unknown error for other exceptions`() = runTest {
        val exception = RuntimeException("Unknown error")
        remote.shouldThrowException = exception

        val result = repository.syncPlays("user123")

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is PlayError.Unknown)
        assertEquals(exception, (error as PlayError.Unknown).throwable)
    }

    @Test
    fun `clearPlays calls local data source`() = runTest {
        local.setPlays(listOf(testPlay))

        repository.clearPlays()

        assertTrue(local.getPlays().isEmpty())
    }

    // --- observePlaysForGame tests ---

    @Test
    fun `observePlaysForGame returns flow from local data source`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Azul", gameId = 123),
            createPlay(localPlayId = 2, gameName = "Azul", gameId = 123)
        )
        local.setPlays(plays)

        val result = repository.observePlaysForGame(123).first()

        assertEquals(plays, result)
    }

    @Test
    fun `observePlaysForGame returns empty list when no plays for game`() = runTest {
        val result = repository.observePlaysForGame(999).first()

        assertTrue(result.isEmpty())
    }

    // --- getPlaysForGame tests ---

    @Test
    fun `getPlaysForGame returns data from local data source`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Azul", gameId = 123),
            createPlay(localPlayId = 2, gameName = "Azul", gameId = 123)
        )
        local.setPlays(plays)

        val result = repository.getPlaysForGame(123)

        assertEquals(plays, result)
    }

    @Test
    fun `getPlaysForGame returns empty list when no plays for game`() = runTest {
        val result = repository.getPlaysForGame(999)

        assertTrue(result.isEmpty())
    }

    // --- observeTotalPlaysCount tests ---

    @Test
    fun `observeTotalPlaysCount returns flow from local data source`() = runTest {
        local.setTotalPlaysCount(10L)

        val result = repository.observeTotalPlaysCount().first()

        assertEquals(10L, result)
    }

    @Test
    fun `observeTotalPlaysCount returns zero when no plays`() = runTest {
        val result = repository.observeTotalPlaysCount().first()

        assertEquals(0L, result)
    }

    // --- observePlaysCountForPeriod tests ---

    @Test
    fun `observePlaysCountForPeriod returns count from local data source`() = runTest {
        val start = parseDateString("2024-01-01")
        val end = parseDateString("2024-02-01")
        local.setPlaysCountForMonth(start, end, 5L)

        val result = repository.observePlaysCountForPeriod(start, end).first()

        assertEquals(5L, result)
    }

    @Test
    fun `observePlaysCountForPeriod returns zero when no plays in period`() = runTest {
        val start = parseDateString("2024-03-01")
        val end = parseDateString("2024-04-01")

        val result = repository.observePlaysCountForPeriod(start, end).first()

        assertEquals(0L, result)
    }

    // --- observeRecentPlays tests ---

    @Test
    fun `observeRecentPlays returns limited plays from local data source`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Catan"),
            createPlay(localPlayId = 2, gameName = "Azul"),
            createPlay(localPlayId = 3, gameName = "Carcassonne"),
        )
        local.setPlays(plays)
        local.setRecentPlays(3, plays)

        val result = repository.observeRecentPlays(3).first()

        assertEquals(plays, result)
    }

    @Test
    fun `observeRecentPlays returns empty list when no plays`() = runTest {
        val result = repository.observeRecentPlays(5).first()

        assertTrue(result.isEmpty())
    }

    // --- observeUniqueGamesCount tests ---

    @Test
    fun `observeUniqueGamesCount returns count from local data source`() = runTest {
        local.setUniqueGamesCount(7L)

        val result = repository.observeUniqueGamesCount().first()

        assertEquals(7L, result)
    }

    @Test
    fun `observeUniqueGamesCount returns zero when no plays`() = runTest {
        val result = repository.observeUniqueGamesCount().first()

        assertEquals(0L, result)
    }

    @Test
    fun `observeUniqueGamesCount updates when unique games count changes`() = runTest {
        local.setUniqueGamesCount(3L)
        val result1 = repository.observeUniqueGamesCount().first()
        assertEquals(3L, result1)

        local.setUniqueGamesCount(5L)
        val result2 = repository.observeUniqueGamesCount().first()
        assertEquals(5L, result2)
    }

    fun createPlayCommand(
        date: Instant = Instant.parse("2024-01-15T20:00:00Z"),
        location: String = "Home",
        gameId: Long = 123,
        gameName: String = "Test Game",
        quantity: Int = 1,
        length: Int = 60,
        incomplete: Boolean = false,
        comments: String? = null,
        players: List<CreatePlayerCommand> = emptyList()
    ) = CreatePlayCommand(
        date = date,
        location = location,
        gameId = gameId,
        gameName = gameName,
        quantity = quantity,
        length = length,
        incomplete = incomplete,
        comments = comments,
        players = players
    )
}
