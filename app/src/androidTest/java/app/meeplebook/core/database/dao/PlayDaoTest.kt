package app.meeplebook.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.core.database.MeepleBookDatabase
import app.meeplebook.core.database.entity.PlayEntity
import app.meeplebook.core.database.entity.PlayerEntity
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
        val gameA = result.find { it.id == 1L }
        assertNotNull(gameA)
        assertEquals("Game A", gameA?.gameName)

        // Game B should be updated
        val gameB = result.find { it.id == 2L }
        assertNotNull(gameB)
        assertEquals("Game B Updated", gameB?.gameName)
        assertEquals(parseDateString("2024-01-10"), gameB?.date)

        // Game C should be new
        val gameC = result.find { it.id == 3L }
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
        assertEquals(2L, result?.id)
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
        assertEquals(1L, result?.play?.id)
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
        assertEquals(2, result[0].play.id)
        assertEquals(1, result[0].players.size)
        assertEquals("Charlie", result[0].players[0].name)
        // Second play
        assertEquals(1, result[1].play.id)
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
    fun observePlaysEmitsSortedByparseDateString() = runTest {
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

        assertEquals(1, result.play.id)
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
            id = id,
            date = date,
            quantity = quantity,
            length = length,
            incomplete = incomplete,
            location = location,
            gameId = gameId,
            gameName = gameName,
            comments = comments
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
        score: String? = null
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
