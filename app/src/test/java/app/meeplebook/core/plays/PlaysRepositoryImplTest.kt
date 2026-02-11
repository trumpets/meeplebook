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
import org.junit.Assert.assertFalse
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

    // --- createPlay tests ---

    @Test
    fun `createPlay inserts play with remoteId null`() = runTest {
        val command = createPlayCommand(
            gameName = "Catan",
            gameId = 123L
        )

        repository.createPlay(command)

        val plays = repository.getPlays()
        assertEquals(1, plays.size)
        val play = plays[0]
        assertTrue(play.playId is PlayId.Local)
        assertEquals("Catan", play.gameName)
        assertEquals(123L, play.gameId)
    }

    @Test
    fun `createPlay inserts play with syncStatus PENDING`() = runTest {
        val command = createPlayCommand(
            gameName = "Azul",
            gameId = 456L
        )

        repository.createPlay(command)

        val plays = repository.getPlays()
        assertEquals(1, plays.size)
        assertEquals(PlaySyncStatus.PENDING, plays[0].syncStatus)
    }

    @Test
    fun `createPlay persists players with correct fields`() = runTest {
        val command = createPlayCommand(
            gameName = "Ticket to Ride",
            gameId = 789L,
            players = listOf(
                createPlayerCommand(
                    name = "Alice",
                    username = "alice123",
                    userId = 111L,
                    score = 100,
                    win = true,
                    startPosition = "1",
                    color = "Red"
                ),
                createPlayerCommand(
                    name = "Bob",
                    username = null,
                    userId = null,
                    score = 85,
                    win = false,
                    startPosition = "2",
                    color = "Blue"
                )
            )
        )

        repository.createPlay(command)

        val plays = repository.getPlays()
        assertEquals(1, plays.size)
        val players = plays[0].players
        assertEquals(2, players.size)
        
        // Verify first player
        assertEquals("Alice", players[0].name)
        assertEquals("alice123", players[0].username)
        assertEquals(111L, players[0].userId)
        assertEquals(100, players[0].score)
        assertTrue(players[0].win)
        assertEquals("1", players[0].startPosition)
        assertEquals("Red", players[0].color)
        
        // Verify second player
        assertEquals("Bob", players[1].name)
        assertEquals(null, players[1].username)
        assertEquals(null, players[1].userId)
        assertEquals(85, players[1].score)
        assertFalse(players[1].win)
        assertEquals("2", players[1].startPosition)
        assertEquals("Blue", players[1].color)
    }

    @Test
    fun `createPlay links players to correct play`() = runTest {
        val command = createPlayCommand(
            gameName = "7 Wonders",
            gameId = 999L,
            players = listOf(
                createPlayerCommand(name = "Charlie"),
                createPlayerCommand(name = "Dana")
            )
        )

        repository.createPlay(command)

        val plays = repository.getPlays()
        assertEquals(1, plays.size)
        val playId = plays[0].playId.localId
        
        // Verify both players are linked to the same play
        plays[0].players.forEach { player ->
            assertEquals(playId, player.playId)
        }
    }

    @Test
    fun `createPlay makes play observable via getPlays`() = runTest {
        val command = createPlayCommand(
            gameName = "Wingspan",
            gameId = 555L,
            date = parseDateString("2024-06-15"),
            location = "Game Night",
            quantity = 1,
            length = 90,
            incomplete = false,
            comments = "Great game!"
        )

        repository.createPlay(command)

        val plays = repository.getPlays()
        assertEquals(1, plays.size)
        
        val play = plays[0]
        assertEquals("Wingspan", play.gameName)
        assertEquals(555L, play.gameId)
        assertEquals(parseDateString("2024-06-15"), play.date)
        assertEquals("Game Night", play.location)
        assertEquals(1, play.quantity)
        assertEquals(90, play.length)
        assertFalse(play.incomplete)
        assertEquals("Great game!", play.comments)
    }

    @Test
    fun `createPlay makes play observable via observePlays`() = runTest {
        val command = createPlayCommand(
            gameName = "Pandemic",
            gameId = 777L
        )

        repository.createPlay(command)

        val plays = repository.observePlays().first()
        assertEquals(1, plays.size)
        assertEquals("Pandemic", plays[0].gameName)
        assertEquals(777L, plays[0].gameId)
    }

    @Test
    fun `createPlay with all optional fields null stores correctly`() = runTest {
        val command = createPlayCommand(
            gameName = "Splendor",
            gameId = 321L,
            location = null,
            length = null,
            comments = null,
            players = emptyList()
        )

        repository.createPlay(command)

        val plays = repository.getPlays()
        assertEquals(1, plays.size)
        
        val play = plays[0]
        assertEquals("Splendor", play.gameName)
        assertEquals(null, play.location)
        assertEquals(null, play.length)
        assertEquals(null, play.comments)
        assertTrue(play.players.isEmpty())
    }

    @Test
    fun `createPlay multiple times creates multiple plays`() = runTest {
        val command1 = createPlayCommand(gameName = "Game 1", gameId = 100L)
        val command2 = createPlayCommand(gameName = "Game 2", gameId = 200L)
        val command3 = createPlayCommand(gameName = "Game 3", gameId = 300L)

        repository.createPlay(command1)
        repository.createPlay(command2)
        repository.createPlay(command3)

        val plays = repository.getPlays()
        assertEquals(3, plays.size)
        assertEquals("Game 1", plays[0].gameName)
        assertEquals("Game 2", plays[1].gameName)
        assertEquals("Game 3", plays[2].gameName)
    }

    // --- observeLocations tests ---

    @Test
    fun `observeLocations returns locations matching query`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Game 1", location = "Home"),
            createPlay(localPlayId = 2, gameName = "Game 2", location = "homebrew"),
            createPlay(localPlayId = 3, gameName = "Game 3", location = "House"),
            createPlay(localPlayId = 4, gameName = "Game 4", location = "Coffee Shop"),
            createPlay(localPlayId = 5, gameName = "Game 5", location = "Hobby Shop")
        )
        local.setPlays(plays)

        val result = repository.observeLocations("Ho").first()

        // Should return distinct locations that start with "Ho", sorted alphabetically (case-insensitive)
        assertEquals(4, result.size)
        assertEquals(listOf("Hobby Shop", "Home", "homebrew", "House"), result)
    }

    @Test
    fun `observeLocations is case-insensitive`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Game 1", location = "Home"),
            createPlay(localPlayId = 2, gameName = "Game 2", location = "homebrew")
        )
        local.setPlays(plays)

        val result = repository.observeLocations("ho").first()

        assertEquals(2, result.size)
        assertTrue(result.contains("Home"))
        assertTrue(result.contains("homebrew"))
    }

    @Test
    fun `observeLocations returns distinct case-preserving results`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Game 1", location = "Home"),
            createPlay(localPlayId = 2, gameName = "Game 2", location = "Home"),
            createPlay(localPlayId = 3, gameName = "Game 3", location = "HOME"),
            createPlay(localPlayId = 4, gameName = "Game 4", location = "home")
        )
        local.setPlays(plays)

        val result = repository.observeLocations("Ho").first()

        assertEquals(3, result.size)
        assertEquals("Home", result[0])
        assertEquals("HOME", result[1])
        assertEquals("home", result[2])
    }

    @Test
    fun `observeLocations sorts results alphabetically case-insensitively`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Game 1", location = "zebra"),
            createPlay(localPlayId = 2, gameName = "Game 2", location = "Apple"),
            createPlay(localPlayId = 3, gameName = "Game 3", location = "banana"),
            createPlay(localPlayId = 4, gameName = "Game 4", location = "Zulu")
        )
        local.setPlays(plays)

        val result = repository.observeLocations("").first()

        // Should be sorted: Apple, banana, zebra, Zulu
        assertEquals(listOf("Apple", "banana", "zebra", "Zulu"), result)
    }

    @Test
    fun `observeLocations limits results to 10`() = runTest {
        val plays = List(15) { i ->
            createPlay(localPlayId = i.toLong() + 1, gameName = "Game ${i + 1}", location = "Location ${i + 1}")
        }
        local.setPlays(plays)

        val result = repository.observeLocations("Lo").first()

        assertEquals(10, result.size)
    }

    @Test
    fun `observeLocations returns empty list for query with no matches`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Game 1", location = "Home"),
            createPlay(localPlayId = 2, gameName = "Game 2", location = "Office")
        )
        local.setPlays(plays)

        val result = repository.observeLocations("Zoo").first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `observeLocations filters out null locations`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Game 1", location = "Home"),
            createPlay(localPlayId = 2, gameName = "Game 2", location = null),
            createPlay(localPlayId = 3, gameName = "Game 3", location = "House")
        )
        local.setPlays(plays)

        val result = repository.observeLocations("H").first()

        assertEquals(2, result.size)
        assertEquals(listOf("Home", "House"), result)
    }

    @Test
    fun `observeLocations returns empty list when no plays exist`() = runTest {
        val result = repository.observeLocations("Any").first()

        assertTrue(result.isEmpty())
    }

    // --- observeRecentLocations tests ---

    @Test
    fun `observeRecentLocations returns locations ordered by most recent play date`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Game A", location = "Home", date = parseDateString("2024-01-01")),
            createPlay(localPlayId = 2, gameName = "Game B", location = "Cafe", date = parseDateString("2024-01-05")),
            createPlay(localPlayId = 3, gameName = "Game C", location = "Home", date = parseDateString("2024-01-03")),
            createPlay(localPlayId = 4, gameName = "Game D", location = "Library", date = parseDateString("2024-01-06")),
            createPlay(localPlayId = 5, gameName = "Game E", location = "Cafe", date = parseDateString("2024-01-02")),
            createPlay(localPlayId = 6, gameName = "Game F", location = "Bar", date = parseDateString("2024-01-04")),
            createPlay(localPlayId = 7, gameName = "Game G", location = "Home", date = parseDateString("2024-01-08"))
        )
        local.setPlays(plays)

        val result = repository.observeRecentLocations().first()

        // Expected order by most recent date for each unique location:
        // Home (2024-01-08), Library (2024-01-06), Cafe (2024-01-05), Bar (2024-01-04)
        assertEquals(listOf("Home", "Library", "Cafe", "Bar"), result)
    }

    @Test
    fun `observeRecentLocations returns distinct case-preserving results`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Game 1", location = "Home", date = parseDateString("2024-01-01")),
            createPlay(localPlayId = 2, gameName = "Game 2", location = "HOME", date = parseDateString("2024-01-02")),
            createPlay(localPlayId = 3, gameName = "Game 3", location = "home", date = parseDateString("2024-01-03"))
        )
        local.setPlays(plays)

        val result = repository.observeRecentLocations().first()

        assertEquals(3, result.size)
        assertEquals("home", result[0])
        assertEquals("HOME", result[1])
        assertEquals("Home", result[2])
    }

    @Test
    fun `observeRecentLocations limits results to 10`() = runTest {
        val plays = List(15) { i ->
            createPlay(
                localPlayId = i.toLong() + 1,
                gameName = "Game ${i + 1}",
                location = "Location ${i + 1}",
                date = parseDateString("2024-01-${String.format("%02d", i + 1)}")
            )
        }
        local.setPlays(plays)

        val result = repository.observeRecentLocations().first()

        assertEquals(10, result.size)
    }

    @Test
    fun `observeRecentLocations filters out null locations`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Game 1", location = "Home", date = parseDateString("2024-01-03")),
            createPlay(localPlayId = 2, gameName = "Game 2", location = null, date = parseDateString("2024-01-02")),
            createPlay(localPlayId = 3, gameName = "Game 3", location = "Office", date = parseDateString("2024-01-01"))
        )
        local.setPlays(plays)

        val result = repository.observeRecentLocations().first()

        assertEquals(2, result.size)
        assertEquals(listOf("Home", "Office"), result)
    }

    @Test
    fun `observeRecentLocations returns empty list when no plays exist`() = runTest {
        val result = repository.observeRecentLocations().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `observeRecentLocations returns empty list when all plays have null locations`() = runTest {
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Game 1", location = null),
            createPlay(localPlayId = 2, gameName = "Game 2", location = null)
        )
        local.setPlays(plays)

        val result = repository.observeRecentLocations().first()

        assertTrue(result.isEmpty())
    }

    // --- Helper functions for createPlay tests ---

    private fun createPlayCommand(
        gameName: String,
        gameId: Long,
        date: Instant = parseDateString("2024-01-01"),
        quantity: Int = 1,
        length: Int? = null,
        incomplete: Boolean = false,
        location: String? = null,
        comments: String? = null,
        players: List<CreatePlayerCommand> = emptyList()
    ): CreatePlayCommand {
        return CreatePlayCommand(
            date = date,
            quantity = quantity,
            length = length,
            incomplete = incomplete,
            location = location,
            gameId = gameId,
            gameName = gameName,
            comments = comments,
            players = players
        )
    }

    private fun createPlayerCommand(
        name: String,
        username: String? = null,
        userId: Long? = null,
        startPosition: String? = null,
        color: String? = null,
        score: Int? = null,
        win: Boolean = false
    ): CreatePlayerCommand {
        return CreatePlayerCommand(
            username = username,
            userId = userId,
            name = name,
            startPosition = startPosition,
            color = color,
            score = score,
            win = win
        )
    }
}
