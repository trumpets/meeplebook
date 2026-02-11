package app.meeplebook.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.core.database.MeepleBookDatabase
import app.meeplebook.core.database.entity.PlayEntity
import app.meeplebook.core.database.entity.PlayerEntity
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.util.parseDateString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Room DAO tests for [PlayDao].
 * Tests only the DAO layer with an in-memory database.
 */
@RunWith(AndroidJUnit4::class)
class PlayDaoTest {

    private lateinit var database: MeepleBookDatabase
    private lateinit var playDao: PlayDao
    private lateinit var playerDao: PlayerDao

    @Before
    fun setUp() {
        // Create an in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MeepleBookDatabase::class.java
        )
            .allowMainThreadQueries() // For test convenience
            .build()

        playDao = database.playDao()
        playerDao = database.playerDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- Test 1: Insert plays & read them back ---

    @Test
    fun insertAndReadPlay() = runTest {
        // Insert a single play
        val play = createTestPlay(
            id = 1,
            date = parseDateString("2024-01-15"),
            gameId = 174430,
            gameName = "Gloomhaven"
        )
        playDao.insert(play)

        // Read it back
        val result = playDao.getPlays()

        assertEquals(1, result.size)
        assertEquals(play, result[0])
    }

    @Test
    fun insertMultiplePlaysAndReadThemBack() = runTest {
        // Insert multiple plays
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-15"), 174430, "Gloomhaven"),
            createTestPlay(2, parseDateString("2024-01-16"), 13, "Catan"),
            createTestPlay(3, parseDateString("2024-01-17"), 120677, "Terra Mystica")
        )
        playDao.insertAll(plays)

        // Read them back
        val result = playDao.getPlays()

        assertEquals(3, result.size)
        assertTrue(result.containsAll(plays))
    }

    @Test
    fun insertPlayWithNullFields() = runTest {
        // Insert play with null optional fields
        val play = createTestPlay(
            id = 100,
            date = parseDateString("2024-01-01"),
            gameId = 1,
            gameName = "Test Game",
            length = null,
            location = null,
            comments = null
        )
        playDao.insert(play)

        // Read it back
        val result = playDao.getPlays()

        assertEquals(1, result.size)
        assertEquals(play, result[0])
        assertNull(result[0].length)
        assertNull(result[0].location)
        assertNull(result[0].comments)
    }

    // --- Test 2: Sorted query by date (descending) ---

    @Test
    fun playsAreSortedByDateDescending() = runTest {
        // Insert plays in random order
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-10"), 1, "Game 1"),
            createTestPlay(2, parseDateString("2024-03-15"), 2, "Game 2"),
            createTestPlay(3, parseDateString("2024-01-01"), 3, "Game 3"),
            createTestPlay(4, parseDateString("2024-02-20"), 4, "Game 4"),
            createTestPlay(5, parseDateString("2024-04-01"), 5, "Game 5")
        )
        playDao.insertAll(plays)

        // Query the plays
        val result = playDao.getPlays()

        // Verify descending order by date
        assertEquals(5, result.size)
        assertEquals(parseDateString("2024-04-01"), result[0].date)
        assertEquals(parseDateString("2024-03-15"), result[1].date)
        assertEquals(parseDateString("2024-02-20"), result[2].date)
        assertEquals(parseDateString("2024-01-10"), result[3].date)
        assertEquals(parseDateString("2024-01-01"), result[4].date)
    }

    // --- Test 3: Upsert behavior with OnConflictStrategy.REPLACE ---

    @Test
    fun upsertReplacesExistingPlay() = runTest {
        // Insert initial play
        val originalPlay = createTestPlay(
            id = 1,
            date = parseDateString("2024-01-01"),
            gameId = 100,
            gameName = "Original Game",
            length = 60
        )
        playDao.insert(originalPlay)

        // Verify initial insert
        var result = playDao.getPlays()
        assertEquals(1, result.size)
        assertEquals("Original Game", result[0].gameName)
        assertEquals(60, result[0].length)

        // Insert same id with different fields (upsert)
        val updatedPlay = createTestPlay(
            id = 1, // Same ID
            date = parseDateString("2024-01-15"),
            gameId = 200,
            gameName = "Updated Game",
            length = 120
        )
        playDao.insert(updatedPlay)

        // Verify the play was updated, not duplicated
        result = playDao.getPlays()
        assertEquals(1, result.size)
        assertEquals("Updated Game", result[0].gameName)
        assertEquals(120, result[0].length)
        assertEquals(parseDateString("2024-01-15"), result[0].date)
    }

    @Test
    fun insertAllWithDuplicatesReplacesExisting() = runTest {
        // Insert initial plays
        val initialPlays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Game A"),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "Game B")
        )
        playDao.insertAll(initialPlays)

        // Insert overlapping plays with some new and some updates
        val newPlays = listOf(
            createTestPlay(2, parseDateString("2024-01-10"), 200, "Game B Updated"), // Update
            createTestPlay(3, parseDateString("2024-01-03"), 300, "Game C") // New
        )
        playDao.insertAll(newPlays)

        // Verify results
        val result = playDao.getPlays()
        assertEquals(3, result.size)
        
        // Game A should remain unchanged
        val gameA = result.find { it.localId == 1L }
        assertNotNull(gameA)
        assertEquals("Game A", gameA?.gameName)

        // Game B should be updated
        val gameB = result.find { it.localId == 2L }
        assertNotNull(gameB)
        assertEquals("Game B Updated", gameB?.gameName)
        assertEquals(parseDateString("2024-01-10"), gameB?.date)

        // Game C should be new
        val gameC = result.find { it.localId == 3L }
        assertNotNull(gameC)
        assertEquals("Game C", gameC?.gameName)
    }

    // --- Test 4: Get play by ID ---

    @Test
    fun getPlayByIdReturnsCorrectPlay() = runTest {
        // Insert multiple plays
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1"),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "Game 2"),
            createTestPlay(3, parseDateString("2024-01-03"), 300, "Game 3")
        )
        playDao.insertAll(plays)

        // Get specific play
        val result = playDao.getPlayById(2)

        assertNotNull(result)
        assertEquals(2L, result?.localId)
        assertEquals("Game 2", result?.gameName)
    }

    @Test
    fun getPlayByIdReturnsNullForNonExistentId() = runTest {
        // Insert a play
        playDao.insert(createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1"))

        // Try to get non-existent play
        val result = playDao.getPlayById(999)

        assertNull(result)
    }

    // --- Test 5: Query plays for specific game ---

    @Test
    fun getPlaysForGameReturnsOnlyMatchingPlays() = runTest {
        // Insert plays for different games
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Game A"),
            createTestPlay(2, parseDateString("2024-01-02"), 100, "Game A"), // Same game
            createTestPlay(3, parseDateString("2024-01-03"), 200, "Game B"),
            createTestPlay(4, parseDateString("2024-01-04"), 100, "Game A")  // Same game
        )
        playDao.insertAll(plays)

        // Query plays for game 100
        val result = playDao.getPlaysForGame(100)

        assertEquals(3, result.size)
        assertTrue(result.all { it.gameId == 100L })
        // Verify descending date order
        assertEquals(parseDateString("2024-01-04"), result[0].date)
        assertEquals(parseDateString("2024-01-02"), result[1].date)
        assertEquals(parseDateString("2024-01-01"), result[2].date)
    }

    @Test
    fun getPlaysForGameReturnsEmptyListForNonExistentGame() = runTest {
        // Insert plays for game 100
        playDao.insert(createTestPlay(1, parseDateString("2024-01-01"), 100, "Game A"))

        // Query plays for non-existent game
        val result = playDao.getPlaysForGame(999)

        assertTrue(result.isEmpty())
    }

    // --- Test 6: Plays with players (relation) ---

    @Test
    fun getPlayWithPlayersIncludesPlayers() = runTest {
        // Insert play
        val play = createTestPlay(1, parseDateString("2024-01-15"), 100, "Test Game")
        playDao.insert(play)

        // Insert players for this play
        val players = listOf(
            createTestPlayer(0, 1, "Alice", true),
            createTestPlayer(0, 1, "Bob", false),
            createTestPlayer(0, 1, "Charlie", false)
        )
        playerDao.insertAll(players)

        // Get play with players
        val result = playDao.getPlayWithPlayersById(1)

        assertNotNull(result)
        assertEquals(1L, result?.play?.localId)
        assertEquals(3, result?.players?.size)
        assertEquals("Alice", result?.players?.get(0)?.name)
    }

    @Test
    fun getPlaysWithPlayersReturnsAllPlaysWithTheirPlayers() = runTest {
        // Insert multiple plays
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1"),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "Game 2")
        )
        playDao.insertAll(plays)

        // Insert players for each play
        playerDao.insertAll(
            listOf(
                createTestPlayer(0, 1, "Alice", true),
                createTestPlayer(0, 1, "Bob", false)
            )
        )
        playerDao.insertAll(
            listOf(
                createTestPlayer(0, 2, "Charlie", true)
            )
        )

        // Get all plays with players
        val result = playDao.getPlaysWithPlayers()

        assertEquals(2, result.size)
        // First play (most recent date)
        assertEquals(2, result[0].play.localId)
        assertEquals(1, result[0].players.size)
        assertEquals("Charlie", result[0].players[0].name)
        // Second play
        assertEquals(1, result[1].play.localId)
        assertEquals(2, result[1].players.size)
    }

    @Test
    fun getPlaysWithPlayersForGameReturnsOnlyMatchingPlays() = runTest {
        // Insert plays for different games
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Game A"),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "Game B"),
            createTestPlay(3, parseDateString("2024-01-03"), 100, "Game A")
        )
        playDao.insertAll(plays)

        // Insert players
        playerDao.insert(createTestPlayer(0, 1, "Alice", true))
        playerDao.insert(createTestPlayer(0, 2, "Bob", true))
        playerDao.insert(createTestPlayer(0, 3, "Charlie", true))

        // Get plays with players for game 100
        val result = playDao.getPlaysWithPlayersForGame(100)

        assertEquals(2, result.size)
        assertTrue(result.all { it.play.gameId == 100L })
    }

    // --- Test 7: Observe plays as Flow ---

    @Test
    fun observePlaysEmitsInitialEmptyList() = runTest {
        // Observe plays without any inserts
        val result = playDao.observePlays().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun observePlaysEmitsAfterInsert() = runTest {
        // Insert plays
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1"),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "Game 2")
        )
        playDao.insertAll(plays)

        // Observe and collect first emission
        val result = playDao.observePlays().first()

        assertEquals(2, result.size)
        assertTrue(result.containsAll(plays))
    }

    @Test
    fun observePlaysEmitsSortedByDate() = runTest {
        // Insert plays in random order
        playDao.insertAll(
            listOf(
                createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1"),
                createTestPlay(2, parseDateString("2024-03-01"), 200, "Game 2"),
                createTestPlay(3, parseDateString("2024-02-01"), 300, "Game 3")
            )
        )

        // Observe the plays
        val result = playDao.observePlays().first()

        // Verify descending date order
        assertEquals(3, result.size)
        assertEquals(parseDateString("2024-03-01"), result[0].date)
        assertEquals(parseDateString("2024-02-01"), result[1].date)
        assertEquals(parseDateString("2024-01-01"), result[2].date)
    }

    @Test
    fun observePlaysForGameEmitsOnlyMatchingPlays() = runTest {
        // Insert plays for different games
        playDao.insertAll(
            listOf(
                createTestPlay(1, parseDateString("2024-01-01"), 100, "Game A"),
                createTestPlay(2, parseDateString("2024-01-02"), 200, "Game B"),
                createTestPlay(3, parseDateString("2024-01-03"), 100, "Game A")
            )
        )

        // Observe plays for game 100
        val result = playDao.observePlaysForGame(100).first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.gameId == 100L })
    }

    @Test
    fun observePlayWithPlayersByIdEmitsPlay() = runTest {
        // Insert play and players
        playDao.insert(createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1"))
        playerDao.insertAll(
            listOf(
                createTestPlayer(0, 1, "Alice", true),
                createTestPlayer(0, 1, "Bob", false)
            )
        )

        // Observe play with players
        val result = playDao.observePlayWithPlayersById(1).first()

        assertEquals(1, result.play.localId)
        assertEquals(2, result.players.size)
    }

    // --- Test 8: Delete operations ---

    @Test
    fun deleteAllRemovesAllPlays() = runTest {
        // Insert plays
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1"),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "Game 2"),
            createTestPlay(3, parseDateString("2024-01-03"), 300, "Game 3")
        )
        playDao.insertAll(plays)

        // Verify plays exist
        var result = playDao.getPlays()
        assertEquals(3, result.size)

        // Delete all
        playDao.deleteAll()

        // Verify all deleted
        result = playDao.getPlays()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteAllOnEmptyDatabaseDoesNotFail() = runTest {
        // Delete on empty database
        playDao.deleteAll()

        // Verify still empty
        val result = playDao.getPlays()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deletingPlayCascadesToPlayers() = runTest {
        // Insert play
        playDao.insert(createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1"))
        
        // Insert players for this play
        playerDao.insertAll(
            listOf(
                createTestPlayer(0, 1, "Alice", true),
                createTestPlayer(0, 1, "Bob", false)
            )
        )

        // Verify players exist
        var players = playerDao.getPlayersForPlay(1)
        assertEquals(2, players.size)

        // Delete the play
        playDao.deleteAll()

        // Verify players were cascade deleted
        players = playerDao.getPlayersForPlay(1)
        assertTrue(players.isEmpty())
    }

    // --- Test 9: Observe total plays count ---

    @Test
    fun observeTotalPlaysCountReturnsCorrectSum() = runTest {
        // Initially should be 0
        var count = playDao.observeTotalPlaysCount().first()
        assertEquals(0L, count)

        // Insert plays with different quantities
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1", quantity = 2),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "Game 2", quantity = 1),
            createTestPlay(3, parseDateString("2024-01-03"), 300, "Game 3", quantity = 3)
        )
        playDao.insertAll(plays)

        // Verify count sums quantities
        count = playDao.observeTotalPlaysCount().first()
        assertEquals(6L, count) // 2 + 1 + 3
    }

    @Test
    fun observeTotalPlaysCountUpdatesAfterDelete() = runTest {
        // Insert plays
        playDao.insertAll(
            listOf(
                createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1", quantity = 2),
                createTestPlay(2, parseDateString("2024-01-02"), 200, "Game 2", quantity = 3)
            )
        )

        // Verify initial count
        var count = playDao.observeTotalPlaysCount().first()
        assertEquals(5L, count)

        // Delete all
        playDao.deleteAll()

        // Verify count updated
        count = playDao.observeTotalPlaysCount().first()
        assertEquals(0L, count)
    }

    // --- Test 10: Observe plays count for month ---

    @Test
    fun observePlaysCountForMonthReturnsCorrectSum() = runTest {
        // Insert plays across different months
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-15"), 100, "Game 1", quantity = 2), // Jan
            createTestPlay(2, parseDateString("2024-01-20"), 200, "Game 2", quantity = 1), // Jan
            createTestPlay(3, parseDateString("2024-02-10"), 300, "Game 3", quantity = 3), // Feb
            createTestPlay(4, parseDateString("2024-02-20"), 400, "Game 4", quantity = 1)  // Feb
        )
        playDao.insertAll(plays)

        // Query for January
        val janStart = parseDateString("2024-01-01")
        val janEnd = parseDateString("2024-02-01")
        val janCount = playDao.observePlaysCountForMonth(janStart, janEnd).first()

        assertEquals(3L, janCount) // 2 + 1

        // Query for February
        val febStart = parseDateString("2024-02-01")
        val febEnd = parseDateString("2024-03-01")
        val febCount = playDao.observePlaysCountForMonth(febStart, febEnd).first()

        assertEquals(4L, febCount) // 3 + 1
    }

    @Test
    fun observePlaysCountForMonthReturnsZeroForEmptyPeriod() = runTest {
        // Insert plays in January
        playDao.insert(createTestPlay(1, parseDateString("2024-01-15"), 100, "Game 1", quantity = 2))

        // Query for March (no plays)
        val marchStart = parseDateString("2024-03-01")
        val marchEnd = parseDateString("2024-04-01")
        val count = playDao.observePlaysCountForMonth(marchStart, marchEnd).first()

        assertEquals(0L, count)
    }

    @Test
    fun observePlaysCountForMonthIncludesStartExcludesEnd() = runTest {
        // Insert plays exactly on boundaries
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-02-01"), 100, "Game 1", quantity = 1), // Start boundary
            createTestPlay(2, parseDateString("2024-02-15"), 200, "Game 2", quantity = 2), // Middle
            createTestPlay(3, parseDateString("2024-03-01"), 300, "Game 3", quantity = 3)  // End boundary
        )
        playDao.insertAll(plays)

        // Query for February (start inclusive, end exclusive)
        val febStart = parseDateString("2024-02-01")
        val febEnd = parseDateString("2024-03-01")
        val count = playDao.observePlaysCountForMonth(febStart, febEnd).first()

        // Should include plays on start date but exclude end date
        assertEquals(3L, count) // 1 + 2 (play 3 is excluded)
    }

    // --- Test 11: Observe unique games count ---

    @Test
    fun observeUniqueGamesCountReturnsZeroForEmptyDatabase() = runTest {
        // Observe unique games count with no plays
        val count = playDao.observeUniqueGamesCount().first()

        assertEquals(0L, count)
    }

    @Test
    fun observeUniqueGamesCountReturnsSingleGame() = runTest {
        // Insert multiple plays for the same game
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Gloomhaven"),
            createTestPlay(2, parseDateString("2024-01-02"), 100, "Gloomhaven"),
            createTestPlay(3, parseDateString("2024-01-03"), 100, "Gloomhaven")
        )
        playDao.insertAll(plays)

        // Verify count is 1 (only 1 distinct game)
        val count = playDao.observeUniqueGamesCount().first()
        assertEquals(1L, count)
    }

    @Test
    fun observeUniqueGamesCountReturnsDistinctGames() = runTest {
        // Insert plays for multiple different games
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1"),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "Game 2"),
            createTestPlay(3, parseDateString("2024-01-03"), 300, "Game 3"),
            createTestPlay(4, parseDateString("2024-01-04"), 100, "Game 1"), // Duplicate
            createTestPlay(5, parseDateString("2024-01-05"), 200, "Game 2")  // Duplicate
        )
        playDao.insertAll(plays)

        // Verify count is 3 (3 distinct games)
        val count = playDao.observeUniqueGamesCount().first()
        assertEquals(3L, count)
    }

    @Test
    fun observeUniqueGamesCountUpdatesReactivelyOnInsert() = runTest {
        // Initially should be 0
        var count = playDao.observeUniqueGamesCount().first()
        assertEquals(0L, count)

        // Insert plays for 2 games
        playDao.insertAll(
            listOf(
                createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1"),
                createTestPlay(2, parseDateString("2024-01-02"), 200, "Game 2")
            )
        )

        // Verify count updated to 2
        count = playDao.observeUniqueGamesCount().first()
        assertEquals(2L, count)

        // Insert play for a new game
        playDao.insert(createTestPlay(3, parseDateString("2024-01-03"), 300, "Game 3"))

        // Verify count updated to 3
        count = playDao.observeUniqueGamesCount().first()
        assertEquals(3L, count)

        // Insert play for an existing game (duplicate gameId)
        playDao.insert(createTestPlay(4, parseDateString("2024-01-04"), 100, "Game 1"))

        // Verify count stays at 3 (no new unique game)
        count = playDao.observeUniqueGamesCount().first()
        assertEquals(3L, count)
    }

    @Test
    fun observeUniqueGamesCountUpdatesReactivelyOnDelete() = runTest {
        // Insert plays for 3 games
        playDao.insertAll(
            listOf(
                createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1"),
                createTestPlay(2, parseDateString("2024-01-02"), 200, "Game 2"),
                createTestPlay(3, parseDateString("2024-01-03"), 300, "Game 3")
            )
        )

        // Verify initial count
        var count = playDao.observeUniqueGamesCount().first()
        assertEquals(3L, count)

        // Delete all plays
        playDao.deleteAll()

        // Verify count updated to 0
        count = playDao.observeUniqueGamesCount().first()
        assertEquals(0L, count)
    }

    // --- Test 12: Observe recent plays with players ---

    @Test
    fun observeRecentPlaysWithPlayersReturnsLimitedResults() = runTest {
        // Insert multiple plays
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1"),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "Game 2"),
            createTestPlay(3, parseDateString("2024-01-03"), 300, "Game 3"),
            createTestPlay(4, parseDateString("2024-01-04"), 400, "Game 4"),
            createTestPlay(5, parseDateString("2024-01-05"), 500, "Game 5")
        )
        playDao.insertAll(plays)

        // Insert players for each play
        for (playId in 1L..5L) {
            playerDao.insert(createTestPlayer(0, playId, "Player $playId", true))
        }

        // Observe recent plays with limit of 3
        val result = playDao.observeRecentPlaysWithPlayers(3).first()

        // Should return 3 most recent plays
        assertEquals(3, result.size)
        // Verify descending order (most recent first)
        assertEquals(5L, result[0].play.localId)
        assertEquals(4L, result[1].play.localId)
        assertEquals(3L, result[2].play.localId)
    }

    @Test
    fun observeRecentPlaysWithPlayersIncludesPlayerData() = runTest {
        // Insert plays
        playDao.insertAll(
            listOf(
                createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1"),
                createTestPlay(2, parseDateString("2024-01-02"), 200, "Game 2")
            )
        )

        // Insert players
        playerDao.insertAll(
            listOf(
                createTestPlayer(0, 1, "Alice", true),
                createTestPlayer(0, 1, "Bob", false),
                createTestPlayer(0, 2, "Charlie", true)
            )
        )

        // Observe recent plays
        val result = playDao.observeRecentPlaysWithPlayers(2).first()

        assertEquals(2, result.size)
        // Most recent play (id=2) should have 1 player
        assertEquals(2L, result[0].play.localId)
        assertEquals(1, result[0].players.size)
        assertEquals("Charlie", result[0].players[0].name)
        
        // Second play (id=1) should have 2 players
        assertEquals(1L, result[1].play.localId)
        assertEquals(2, result[1].players.size)
    }

    @Test
    fun observeRecentPlaysWithPlayersReturnsEmptyWhenNoPlays() = runTest {
        // Observe with no plays
        val result = playDao.observeRecentPlaysWithPlayers(5).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun observeRecentPlaysWithPlayersHandlesLimitLargerThanCount() = runTest {
        // Insert 2 plays
        playDao.insertAll(
            listOf(
                createTestPlay(1, parseDateString("2024-01-01"), 100, "Game 1"),
                createTestPlay(2, parseDateString("2024-01-02"), 200, "Game 2")
            )
        )

        // Request limit of 10 (more than available)
        val result = playDao.observeRecentPlaysWithPlayers(10).first()

        // Should return all 2 plays
        assertEquals(2, result.size)
    }

    // --- Test 12: Observe plays by game name or location ---

    @Test
    fun observePlaysWithPlayersByGameNameOrLocationMatchesOnGameName() = runTest {
        // Insert plays with different game names
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Gloomhaven"),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "Catan"),
            createTestPlay(3, parseDateString("2024-01-03"), 300, "Gloomhaven: Jaws of the Lion"),
            createTestPlay(4, parseDateString("2024-01-04"), 400, "Pandemic")
        )
        playDao.insertAll(plays)

        // Add players to each play
        for (playId in 1L..4L) {
            playerDao.insert(createTestPlayer(0, playId, "Player $playId", true))
        }

        // Search for "Gloom" should match both Gloomhaven games
        val result = playDao.observePlaysWithPlayersByGameNameOrLocation("Gloom").first()

        assertEquals(2, result.size)
        assertTrue(result.any { it.play.gameName == "Gloomhaven" })
        assertTrue(result.any { it.play.gameName == "Gloomhaven: Jaws of the Lion" })
    }

    @Test
    fun observePlaysWithPlayersByGameNameOrLocationMatchesOnLocation() = runTest {
        // Insert plays with different locations
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Game A", location = "Home"),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "Game B", location = "Coffee Shop"),
            createTestPlay(3, parseDateString("2024-01-03"), 300, "Game C", location = "Friend's Home"),
            createTestPlay(4, parseDateString("2024-01-04"), 400, "Game D", location = null)
        )
        playDao.insertAll(plays)

        // Add players to each play
        for (playId in 1L..4L) {
            playerDao.insert(createTestPlayer(0, playId, "Player $playId", true))
        }

        // Search for "Home" should match plays 1 and 3
        val result = playDao.observePlaysWithPlayersByGameNameOrLocation("Home").first()

        assertEquals(2, result.size)
        assertTrue(result.any { it.play.location == "Home" })
        assertTrue(result.any { it.play.location == "Friend's Home" })
    }

    @Test
    fun observePlaysWithPlayersByGameNameOrLocationMatchesOnBothGameNameAndLocation() = runTest {
        // Insert plays where query matches either game name or location
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Catan", location = "Home"),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "Gloomhaven", location = "Coffee Shop"),
            createTestPlay(3, parseDateString("2024-01-03"), 300, "Pandemic", location = "Catan Cafe"),
            createTestPlay(4, parseDateString("2024-01-04"), 400, "Ticket to Ride", location = "Library")
        )
        playDao.insertAll(plays)

        // Add players to each play
        for (playId in 1L..4L) {
            playerDao.insert(createTestPlayer(0, playId, "Player $playId", true))
        }

        // Search for "Catan" should match play 1 (game name) and play 3 (location)
        val result = playDao.observePlaysWithPlayersByGameNameOrLocation("Catan").first()

        assertEquals(2, result.size)
        assertTrue(result.any { it.play.gameName == "Catan" })
        assertTrue(result.any { it.play.location == "Catan Cafe" })
    }

    @Test
    fun observePlaysWithPlayersByGameNameOrLocationIsSortedByDateDescending() = runTest {
        // Insert plays in random order
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-10"), 100, "Game Alpha"),
            createTestPlay(2, parseDateString("2024-03-15"), 200, "Game Beta"),
            createTestPlay(3, parseDateString("2024-01-01"), 300, "Game Gamma"),
            createTestPlay(4, parseDateString("2024-04-01"), 400, "Game Delta"),
            createTestPlay(5, parseDateString("2024-02-20"), 500, "Game Epsilon")
        )
        playDao.insertAll(plays)

        // Add players to each play
        for (playId in 1L..5L) {
            playerDao.insert(createTestPlayer(0, playId, "Player $playId", true))
        }

        // Search for "Game" (should match all)
        val result = playDao.observePlaysWithPlayersByGameNameOrLocation("Game").first()

        // Verify descending order by date
        assertEquals(5, result.size)
        assertEquals(parseDateString("2024-04-01"), result[0].play.date)
        assertEquals(parseDateString("2024-03-15"), result[1].play.date)
        assertEquals(parseDateString("2024-02-20"), result[2].play.date)
        assertEquals(parseDateString("2024-01-10"), result[3].play.date)
        assertEquals(parseDateString("2024-01-01"), result[4].play.date)
    }

    @Test
    fun observePlaysWithPlayersByGameNameOrLocationIsCaseInsensitive() = runTest {
        // Insert plays with mixed case game names and locations
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Gloomhaven", location = "HOME"),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "CATAN", location = "coffee shop"),
            createTestPlay(3, parseDateString("2024-01-03"), 300, "PaNdEmIc", location = "Library")
        )
        playDao.insertAll(plays)

        // Add players to each play
        for (playId in 1L..3L) {
            playerDao.insert(createTestPlayer(0, playId, "Player $playId", true))
        }

        // Search with lowercase should match regardless of case
        val gloomResult = playDao.observePlaysWithPlayersByGameNameOrLocation("gloom").first()
        assertEquals(1, gloomResult.size)
        assertEquals("Gloomhaven", gloomResult[0].play.gameName)

        val catanResult = playDao.observePlaysWithPlayersByGameNameOrLocation("catan").first()
        assertEquals(1, catanResult.size)
        assertEquals("CATAN", catanResult[0].play.gameName)

        val homeResult = playDao.observePlaysWithPlayersByGameNameOrLocation("home").first()
        assertEquals(1, homeResult.size)
        assertEquals("HOME", homeResult[0].play.location)

        val coffeeResult = playDao.observePlaysWithPlayersByGameNameOrLocation("COFFEE").first()
        assertEquals(1, coffeeResult.size)
        assertEquals("coffee shop", coffeeResult[0].play.location)
    }

    @Test
    fun observePlaysWithPlayersByGameNameOrLocationIncludesPlayerData() = runTest {
        // Insert plays
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Gloomhaven", location = "Home"),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "Catan", location = "Coffee Shop")
        )
        playDao.insertAll(plays)

        // Insert players for each play
        playerDao.insertAll(
            listOf(
                createTestPlayer(0, 1, "Alice", true),
                createTestPlayer(0, 1, "Bob", false),
                createTestPlayer(0, 2, "Charlie", true)
            )
        )

        // Search for "Gloomhaven"
        val result = playDao.observePlaysWithPlayersByGameNameOrLocation("Gloomhaven").first()

        assertEquals(1, result.size)
        assertEquals("Gloomhaven", result[0].play.gameName)
        assertEquals(2, result[0].players.size)
        assertTrue(result[0].players.any { it.name == "Alice" })
        assertTrue(result[0].players.any { it.name == "Bob" })
    }

    @Test
    fun observePlaysWithPlayersByGameNameOrLocationReturnsEmptyForNoMatches() = runTest {
        // Insert plays
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Gloomhaven", location = "Home"),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "Catan", location = "Coffee Shop")
        )
        playDao.insertAll(plays)

        // Add players to each play
        for (playId in 1L..2L) {
            playerDao.insert(createTestPlayer(0, playId, "Player $playId", true))
        }

        // Search for non-existent query
        val result = playDao.observePlaysWithPlayersByGameNameOrLocation("NonExistent").first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun observePlaysWithPlayersByGameNameOrLocationHandlesNullLocation() = runTest {
        // Insert plays with null locations
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-01"), 100, "Gloomhaven", location = null),
            createTestPlay(2, parseDateString("2024-01-02"), 200, "Catan", location = "Home")
        )
        playDao.insertAll(plays)

        // Add players to each play
        for (playId in 1L..2L) {
            playerDao.insert(createTestPlayer(0, playId, "Player $playId", true))
        }

        // Search for location string - should only match play 2
        val result = playDao.observePlaysWithPlayersByGameNameOrLocation("Home").first()

        assertEquals(1, result.size)
        assertEquals("Catan", result[0].play.gameName)
        assertEquals("Home", result[0].play.location)
    }

    // --- Test 13: getRemotePlays ---

    @Test
    fun getRemotePlaysReturnsOnlyPlaysWithRemoteIds() = runTest {
        // Insert plays with and without remote IDs
        val playWithRemoteId1 = PlayEntity(
            localId = 1,
            remoteId = 1001,
            date = parseDateString("2024-01-15"),
            quantity = 1,
            length = 60,
            incomplete = false,
            location = null,
            gameId = 100,
            gameName = "Game 1",
            comments = null,
            syncStatus = PlaySyncStatus.SYNCED
        )
        val playWithRemoteId2 = PlayEntity(
            localId = 2,
            remoteId = 1002,
            date = parseDateString("2024-01-16"),
            quantity = 1,
            length = 60,
            incomplete = false,
            location = null,
            gameId = 200,
            gameName = "Game 2",
            comments = null,
            syncStatus = PlaySyncStatus.SYNCED
        )
        val playWithoutRemoteId = PlayEntity(
            localId = 3,
            remoteId = null,
            date = parseDateString("2024-01-17"),
            quantity = 1,
            length = 60,
            incomplete = false,
            location = null,
            gameId = 300,
            gameName = "Game 3",
            comments = null,
            syncStatus = PlaySyncStatus.PENDING
        )

        playDao.insertAll(listOf(playWithRemoteId1, playWithRemoteId2, playWithoutRemoteId))

        // Get remote plays
        val result = playDao.getRemotePlays()

        // Should only return plays with remote IDs
        assertEquals(2, result.size)
        assertTrue(result.all { it.remoteId != null })
        assertTrue(result.any { it.localId == 1L })
        assertTrue(result.any { it.localId == 2L })
    }

    @Test
    fun getRemotePlaysReturnsEmptyListWhenNoRemotePlays() = runTest {
        // Insert only local plays
        val localPlay = PlayEntity(
            localId = 1,
            remoteId = null,
            date = parseDateString("2024-01-15"),
            quantity = 1,
            length = 60,
            incomplete = false,
            location = null,
            gameId = 100,
            gameName = "Local Game",
            comments = null,
            syncStatus = PlaySyncStatus.PENDING
        )
        playDao.insert(localPlay)

        // Get remote plays
        val result = playDao.getRemotePlays()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getRemotePlaysReturnsSortedByDateDescending() = runTest {
        // Insert remote plays with different dates
        val plays = listOf(
            PlayEntity(
                localId = 1,
                remoteId = 1001,
                date = parseDateString("2024-01-15"),
                quantity = 1,
                length = 60,
                incomplete = false,
                location = null,
                gameId = 100,
                gameName = "Game 1",
                comments = null,
                syncStatus = PlaySyncStatus.SYNCED
            ),
            PlayEntity(
                localId = 2,
                remoteId = 1002,
                date = parseDateString("2024-01-20"),
                quantity = 1,
                length = 60,
                incomplete = false,
                location = null,
                gameId = 200,
                gameName = "Game 2",
                comments = null,
                syncStatus = PlaySyncStatus.SYNCED
            ),
            PlayEntity(
                localId = 3,
                remoteId = 1003,
                date = parseDateString("2024-01-10"),
                quantity = 1,
                length = 60,
                incomplete = false,
                location = null,
                gameId = 300,
                gameName = "Game 3",
                comments = null,
                syncStatus = PlaySyncStatus.SYNCED
            )
        )
        playDao.insertAll(plays)

        // Get remote plays
        val result = playDao.getRemotePlays()

        // Should be sorted by date descending
        assertEquals(3, result.size)
        assertEquals(parseDateString("2024-01-20"), result[0].date)
        assertEquals(parseDateString("2024-01-15"), result[1].date)
        assertEquals(parseDateString("2024-01-10"), result[2].date)
    }

    // --- Test 14: getByRemoteIds ---

    @Test
    fun getByRemoteIdsReturnsMatchingPlays() = runTest {
        // Insert plays with remote IDs
        val plays = listOf(
            PlayEntity(
                localId = 1,
                remoteId = 1001,
                date = parseDateString("2024-01-15"),
                quantity = 1,
                length = 60,
                incomplete = false,
                location = null,
                gameId = 100,
                gameName = "Game 1",
                comments = null,
                syncStatus = PlaySyncStatus.SYNCED
            ),
            PlayEntity(
                localId = 2,
                remoteId = 1002,
                date = parseDateString("2024-01-16"),
                quantity = 1,
                length = 60,
                incomplete = false,
                location = null,
                gameId = 200,
                gameName = "Game 2",
                comments = null,
                syncStatus = PlaySyncStatus.SYNCED
            ),
            PlayEntity(
                localId = 3,
                remoteId = 1003,
                date = parseDateString("2024-01-17"),
                quantity = 1,
                length = 60,
                incomplete = false,
                location = null,
                gameId = 300,
                gameName = "Game 3",
                comments = null,
                syncStatus = PlaySyncStatus.SYNCED
            )
        )
        playDao.insertAll(plays)

        // Get by specific remote IDs
        val result = playDao.getByRemoteIds(listOf(1001, 1003))

        // Should return only matching plays
        assertEquals(2, result.size)
        assertTrue(result.any { it.remoteId == 1001L })
        assertTrue(result.any { it.remoteId == 1003L })
    }

    @Test
    fun getByRemoteIdsReturnsEmptyListForNoMatches() = runTest {
        // Insert plays
        val play = PlayEntity(
            localId = 1,
            remoteId = 1001,
            date = parseDateString("2024-01-15"),
            quantity = 1,
            length = 60,
            incomplete = false,
            location = null,
            gameId = 100,
            gameName = "Game 1",
            comments = null,
            syncStatus = PlaySyncStatus.SYNCED
        )
        playDao.insert(play)

        // Query for non-existent remote IDs
        val result = playDao.getByRemoteIds(listOf(9999, 8888))

        assertTrue(result.isEmpty())
    }

    @Test
    fun getByRemoteIdsHandlesEmptyList() = runTest {
        // Insert plays
        val play = PlayEntity(
            localId = 1,
            remoteId = 1001,
            date = parseDateString("2024-01-15"),
            quantity = 1,
            length = 60,
            incomplete = false,
            location = null,
            gameId = 100,
            gameName = "Game 1",
            comments = null,
            syncStatus = PlaySyncStatus.SYNCED
        )
        playDao.insert(play)

        // Query with empty list
        val result = playDao.getByRemoteIds(emptyList())

        assertTrue(result.isEmpty())
    }

    // --- Test 15: deleteByRemoteIds ---

    @Test
    fun deleteByRemoteIdsRemovesMatchingPlays() = runTest {
        // Insert plays with remote IDs
        val plays = listOf(
            PlayEntity(
                localId = 1,
                remoteId = 1001,
                date = parseDateString("2024-01-15"),
                quantity = 1,
                length = 60,
                incomplete = false,
                location = null,
                gameId = 100,
                gameName = "Game 1",
                comments = null,
                syncStatus = PlaySyncStatus.SYNCED
            ),
            PlayEntity(
                localId = 2,
                remoteId = 1002,
                date = parseDateString("2024-01-16"),
                quantity = 1,
                length = 60,
                incomplete = false,
                location = null,
                gameId = 200,
                gameName = "Game 2",
                comments = null,
                syncStatus = PlaySyncStatus.SYNCED
            ),
            PlayEntity(
                localId = 3,
                remoteId = 1003,
                date = parseDateString("2024-01-17"),
                quantity = 1,
                length = 60,
                incomplete = false,
                location = null,
                gameId = 300,
                gameName = "Game 3",
                comments = null,
                syncStatus = PlaySyncStatus.SYNCED
            )
        )
        playDao.insertAll(plays)

        // Delete by specific remote IDs
        playDao.deleteByRemoteIds(listOf(1001, 1003))

        // Verify deleted
        val remaining = playDao.getPlays()
        assertEquals(1, remaining.size)
        assertEquals(1002L, remaining[0].remoteId)
    }

    @Test
    fun deleteByRemoteIdsDoesNotAffectLocalOnlyPlays() = runTest {
        // Insert mix of remote and local plays
        val plays = listOf(
            PlayEntity(
                localId = 1,
                remoteId = 1001,
                date = parseDateString("2024-01-15"),
                quantity = 1,
                length = 60,
                incomplete = false,
                location = null,
                gameId = 100,
                gameName = "Remote Game",
                comments = null,
                syncStatus = PlaySyncStatus.SYNCED
            ),
            PlayEntity(
                localId = 2,
                remoteId = null,
                date = parseDateString("2024-01-16"),
                quantity = 1,
                length = 60,
                incomplete = false,
                location = null,
                gameId = 200,
                gameName = "Local Game",
                comments = null,
                syncStatus = PlaySyncStatus.PENDING
            )
        )
        playDao.insertAll(plays)

        // Delete by remote ID
        playDao.deleteByRemoteIds(listOf(1001))

        // Verify local play still exists
        val remaining = playDao.getPlays()
        assertEquals(1, remaining.size)
        assertEquals(2L, remaining[0].localId)
        assertNull(remaining[0].remoteId)
    }

    @Test
    fun deleteByRemoteIdsHandlesEmptyList() = runTest {
        // Insert plays
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-15"), 100, "Game 1"),
            createTestPlay(2, parseDateString("2024-01-16"), 200, "Game 2")
        )
        playDao.insertAll(plays)

        // Delete with empty list - should not affect any plays
        playDao.deleteByRemoteIds(emptyList())

        // Verify all plays still exist
        val remaining = playDao.getPlays()
        assertEquals(2, remaining.size)
    }

    // --- Test 16: upsertAll ---

    @Test
    fun upsertAllInsertsNewPlays() = runTest {
        // Upsert new plays
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-15"), 100, "Game 1"),
            createTestPlay(2, parseDateString("2024-01-16"), 200, "Game 2")
        )
        playDao.upsertAll(plays)

        // Verify inserted
        val result = playDao.getPlays()
        assertEquals(2, result.size)
    }

    @Test
    fun upsertAllUpdatesExistingPlays() = runTest {
        // Insert initial play
        val initialPlay = PlayEntity(
            localId = 1,
            remoteId = 1001,
            date = parseDateString("2024-01-15"),
            quantity = 1,
            length = 60,
            incomplete = false,
            location = "Initial Location",
            gameId = 100,
            gameName = "Initial Name",
            comments = "Initial comments",
            syncStatus = PlaySyncStatus.SYNCED
        )
        playDao.insert(initialPlay)

        // Verify initial state
        var result = playDao.getPlayById(1)
        assertNotNull(result)
        assertEquals("Initial Location", result?.location)
        assertEquals("Initial Name", result?.gameName)

        // Upsert with same localId but different fields
        val updatedPlay = PlayEntity(
            localId = 1,
            remoteId = 1001,
            date = parseDateString("2024-01-15"),
            quantity = 2,
            length = 90,
            incomplete = true,
            location = "Updated Location",
            gameId = 100,
            gameName = "Updated Name",
            comments = "Updated comments",
            syncStatus = PlaySyncStatus.PENDING
        )
        playDao.upsertAll(listOf(updatedPlay))

        // Verify updated
        result = playDao.getPlayById(1)
        assertNotNull(result)
        assertEquals("Updated Location", result?.location)
        assertEquals("Updated Name", result?.gameName)
        assertEquals(2, result?.quantity)
        assertEquals(90, result?.length)
        assertTrue(result?.incomplete ?: false)
        assertEquals("Updated comments", result?.comments)
        assertEquals(PlaySyncStatus.PENDING, result?.syncStatus)
    }

    @Test
    fun upsertAllHandlesMixOfNewAndExistingPlays() = runTest {
        // Insert initial play
        val existingPlay = createTestPlay(1, parseDateString("2024-01-15"), 100, "Existing Game")
        playDao.insert(existingPlay)

        // Upsert mix of existing and new
        val plays = listOf(
            createTestPlay(1, parseDateString("2024-01-15"), 100, "Updated Game"),
            createTestPlay(2, parseDateString("2024-01-16"), 200, "New Game")
        )
        playDao.upsertAll(plays)

        // Verify both exist with correct data
        val result = playDao.getPlays()
        assertEquals(2, result.size)

        val play1 = result.find { it.localId == 1L }
        assertNotNull(play1)
        assertEquals("Updated Game", play1?.gameName)

        val play2 = result.find { it.localId == 2L }
        assertNotNull(play2)
        assertEquals("New Game", play2?.gameName)
    }

    // --- Helper functions ---

    private fun createTestPlay(
        id: Long,
        date: Instant,
        gameId: Long,
        gameName: String,
        quantity: Int = 1,
        length: Int? = 60,
        incomplete: Boolean = false,
        location: String? = null,
        comments: String? = null
    ): PlayEntity {
        return PlayEntity(
            localId = id,
            remoteId = id * 100,
            date = date,
            quantity = quantity,
            length = length,
            incomplete = incomplete,
            location = location,
            gameId = gameId,
            gameName = gameName,
            comments = comments,
            syncStatus = PlaySyncStatus.SYNCED
        )
    }

    private fun createTestPlayer(
        id: Long,
        playId: Long,
        name: String,
        win: Boolean,
        username: String? = null,
        userId: Long? = null,
        startPosition: String? = null,
        color: String? = null,
        score: Int? = null
    ): PlayerEntity {
        return PlayerEntity(
            id = id,
            playId = playId,
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
