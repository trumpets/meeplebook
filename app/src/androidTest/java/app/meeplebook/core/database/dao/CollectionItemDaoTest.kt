package app.meeplebook.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.database.MeepleBookDatabase
import app.meeplebook.core.database.entity.CollectionItemEntity
import app.meeplebook.core.util.parseDateString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Room DAO tests for [CollectionItemDao].
 * Tests only the DAO layer with an in-memory database.
 */
@RunWith(AndroidJUnit4::class)
class CollectionItemDaoTest {

    private lateinit var database: MeepleBookDatabase
    private lateinit var dao: CollectionItemDao

    @Before
    fun setUp() {
        // Create an in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MeepleBookDatabase::class.java
        )
            .allowMainThreadQueries() // For test convenience
            .build()

        dao = database.collectionItemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- Test 1: Insert items & read them back ---

    @Test
    fun insertAndReadItems() = runTest {
        // Insert a single item
        val item1 = createTestEntity(
            gameId = 1,
            name = "Catan",
            subtype = GameSubtype.BOARDGAME,
            yearPublished = 1995,
            thumbnail = "https://example.com/catan.jpg"
        )
        dao.insert(item1)

        // Read it back
        val result = dao.getCollection()

        assertEquals(1, result.size)
        assertEquals(item1, result[0])
    }

    @Test
    fun insertMultipleItemsAndReadThemBack() = runTest {
        // Insert multiple items
        val items = listOf(
            createTestEntity(1, "Catan", GameSubtype.BOARDGAME, 1995, "thumb1.jpg"),
            createTestEntity(2, "Ticket to Ride", GameSubtype.BOARDGAME, 2004, "thumb2.jpg"),
            createTestEntity(3, "Catan Expansion", GameSubtype.BOARDGAME_EXPANSION, 1996, null)
        )
        dao.insertAll(items)

        // Read them back
        val result = dao.getCollection()

        assertEquals(3, result.size)
        assertTrue(result.containsAll(items))
    }

    @Test
    fun insertItemWithNullFields() = runTest {
        // Insert item with null optional fields
        val item = createTestEntity(
            gameId = 100,
            name = "Game Without Thumbnail",
            subtype = GameSubtype.BOARDGAME,
            yearPublished = null,
            thumbnail = null
        )
        dao.insert(item)

        // Read it back
        val result = dao.getCollection()

        assertEquals(1, result.size)
        assertEquals(item, result[0])
        assertEquals(null, result[0].yearPublished)
        assertEquals(null, result[0].thumbnail)
    }

    // --- Test 2: Sorted query by name (alphabetical) ---

    @Test
    fun itemsAreSortedByName() = runTest {
        // Insert items in random order
        val items = listOf(
            createTestEntity(3, "Zombicide", GameSubtype.BOARDGAME),
            createTestEntity(1, "Agricola", GameSubtype.BOARDGAME),
            createTestEntity(5, "Pandemic", GameSubtype.BOARDGAME),
            createTestEntity(2, "Brass Birmingham", GameSubtype.BOARDGAME),
            createTestEntity(4, "Gloomhaven", GameSubtype.BOARDGAME)
        )
        dao.insertAll(items)

        // Query the collection
        val result = dao.getCollection()

        // Verify alphabetical order by name
        assertEquals(5, result.size)
        assertEquals("Agricola", result[0].name)
        assertEquals("Brass Birmingham", result[1].name)
        assertEquals("Gloomhaven", result[2].name)
        assertEquals("Pandemic", result[3].name)
        assertEquals("Zombicide", result[4].name)
    }

    @Test
    fun sortingHandlesMixedCase() = runTest {
        // Insert items with different cases
        val items = listOf(
            createTestEntity(1, "catan", GameSubtype.BOARDGAME),
            createTestEntity(2, "Azul", GameSubtype.BOARDGAME),
            createTestEntity(3, "WINGSPAN", GameSubtype.BOARDGAME),
            createTestEntity(4, "Brass", GameSubtype.BOARDGAME)
        )
        dao.insertAll(items)

        // Query the collection
        val result = dao.getCollection()

        // Verify alphabetical order by name (SQLite uses BINARY collation by default)
        assertEquals(4, result.size)
        assertEquals("Azul", result[0].name)
        assertEquals("Brass", result[1].name)
        assertEquals("WINGSPAN", result[2].name)
        assertEquals("catan", result[3].name)
    }

    // --- Test 3: Upsert behavior with OnConflictStrategy.REPLACE ---

    @Test
    fun upsertReplacesExistingItem() = runTest {
        // Insert initial item
        val originalItem = createTestEntity(
            gameId = 1,
            name = "Original Name",
            subtype = GameSubtype.BOARDGAME,
            yearPublished = 2000,
            thumbnail = "original.jpg"
        )
        dao.insert(originalItem)

        // Verify initial insert
        var result = dao.getCollection()
        assertEquals(1, result.size)
        assertEquals("Original Name", result[0].name)
        assertEquals(2000, result[0].yearPublished)

        // Insert same gameId with different fields (upsert)
        val updatedItem = createTestEntity(
            gameId = 1, // Same ID
            name = "Updated Name",
            subtype = GameSubtype.BOARDGAME_EXPANSION,
            yearPublished = 2020,
            thumbnail = "updated.jpg"
        )
        dao.insert(updatedItem)

        // Verify the item was updated, not duplicated
        result = dao.getCollection()
        assertEquals(1, result.size)
        assertEquals("Updated Name", result[0].name)
        assertEquals(2020, result[0].yearPublished)
        assertEquals("updated.jpg", result[0].thumbnail)
        assertEquals(GameSubtype.BOARDGAME_EXPANSION, result[0].subtype)
    }

    @Test
    fun insertAllWithDuplicatesReplacesExisting() = runTest {
        // Insert initial items
        val initialItems = listOf(
            createTestEntity(1, "Game A", GameSubtype.BOARDGAME, 2000),
            createTestEntity(2, "Game B", GameSubtype.BOARDGAME, 2001)
        )
        dao.insertAll(initialItems)

        // Insert overlapping items with some new and some updates
        val newItems = listOf(
            createTestEntity(2, "Game B Updated", GameSubtype.BOARDGAME, 2022), // Update
            createTestEntity(3, "Game C", GameSubtype.BOARDGAME, 2023) // New
        )
        dao.insertAll(newItems)

        // Verify results
        val result = dao.getCollection()
        assertEquals(3, result.size)
        
        // Game A should remain unchanged
        val gameA = result.find { it.gameId == 1L }
        assertNotNull(gameA)
        assertEquals("Game A", gameA?.name)
        assertEquals(2000, gameA?.yearPublished)

        // Game B should be updated
        val gameB = result.find { it.gameId == 2L }
        assertNotNull(gameB)
        assertEquals("Game B Updated", gameB?.name)
        assertEquals(2022, gameB?.yearPublished)

        // Game C should be new
        val gameC = result.find { it.gameId == 3L }
        assertNotNull(gameC)
        assertEquals("Game C", gameC?.name)
    }

    // --- Test 4: Replace full collection ---

    @Test
    fun replaceCollectionDeletesOldAndInsertsNew() = runTest {
        // Insert initial collection
        val oldItems = listOf(
            createTestEntity(1, "Old Game 1", GameSubtype.BOARDGAME),
            createTestEntity(2, "Old Game 2", GameSubtype.BOARDGAME),
            createTestEntity(3, "Old Game 3", GameSubtype.BOARDGAME)
        )
        dao.insertAll(oldItems)

        // Verify initial state
        var result = dao.getCollection()
        assertEquals(3, result.size)

        // Replace with new collection
        val newItems = listOf(
            createTestEntity(10, "New Game 1", GameSubtype.BOARDGAME),
            createTestEntity(20, "New Game 2", GameSubtype.BOARDGAME_EXPANSION)
        )
        dao.replaceCollection(newItems)

        // Verify only new items exist
        result = dao.getCollection()
        assertEquals(2, result.size)
        assertTrue(result.none { it.gameId in listOf(1L, 2L, 3L) })
        assertTrue(result.any { it.gameId == 10L && it.name == "New Game 1" })
        assertTrue(result.any { it.gameId == 20L && it.name == "New Game 2" })
    }

    @Test
    fun replaceCollectionWithEmptyListClearsAll() = runTest {
        // Insert initial items
        val items = listOf(
            createTestEntity(1, "Game 1", GameSubtype.BOARDGAME),
            createTestEntity(2, "Game 2", GameSubtype.BOARDGAME)
        )
        dao.insertAll(items)

        // Verify initial state
        var result = dao.getCollection()
        assertEquals(2, result.size)

        // Replace with empty list
        dao.replaceCollection(emptyList())

        // Verify all items are deleted
        result = dao.getCollection()
        assertTrue(result.isEmpty())
    }

    @Test
    fun replaceCollectionIsTransactional() = runTest {
        // Insert initial items
        val initialItems = listOf(
            createTestEntity(1, "Game 1", GameSubtype.BOARDGAME)
        )
        dao.insertAll(initialItems)

        // Replace with new items
        val newItems = listOf(
            createTestEntity(2, "Game 2", GameSubtype.BOARDGAME),
            createTestEntity(3, "Game 3", GameSubtype.BOARDGAME)
        )
        dao.replaceCollection(newItems)

        // Verify the operation completed atomically
        val result = dao.getCollection()
        assertEquals(2, result.size)
        assertTrue(result.none { it.gameId == 1L })
        assertTrue(result.all { it.gameId in listOf(2L, 3L) })
    }

    // --- Test 5: Observe collection as Flow ---

    @Test
    fun observeCollectionEmitsInitialEmptyList() = runTest {
        // Observe the collection without any inserts
        val result = dao.observeCollection().first()

        // Verify empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun observeCollectionEmitsAfterInsert() = runTest {
        // Insert items
        val items = listOf(
            createTestEntity(1, "Game 1", GameSubtype.BOARDGAME),
            createTestEntity(2, "Game 2", GameSubtype.BOARDGAME)
        )
        dao.insertAll(items)

        // Observe and collect first emission
        val result = dao.observeCollection().first()

        // Verify items are emitted
        assertEquals(2, result.size)
        assertTrue(result.containsAll(items))
    }

    @Test
    fun observeCollectionEmitsSortedByName() = runTest {
        // Insert items in random order
        dao.insertAll(
            listOf(
                createTestEntity(3, "Zombicide", GameSubtype.BOARDGAME),
                createTestEntity(1, "Agricola", GameSubtype.BOARDGAME),
                createTestEntity(2, "Pandemic", GameSubtype.BOARDGAME)
            )
        )

        // Observe the collection
        val result = dao.observeCollection().first()

        // Verify alphabetical order
        assertEquals(3, result.size)
        assertEquals("Agricola", result[0].name)
        assertEquals("Pandemic", result[1].name)
        assertEquals("Zombicide", result[2].name)
    }

    @Test
    fun observeCollectionEmitsUpdatesAfterReplace() = runTest {
        // Insert initial items
        dao.insertAll(
            listOf(
                createTestEntity(1, "Game 1", GameSubtype.BOARDGAME)
            )
        )

        // Verify initial state - Room's Flow emits current database state immediately
        var result = dao.observeCollection().first()
        assertEquals(1, result.size)
        assertEquals(1, result[0].gameId)

        // Replace collection with new items
        dao.replaceCollection(
            listOf(
                createTestEntity(2, "Game 2", GameSubtype.BOARDGAME),
                createTestEntity(3, "Game 3", GameSubtype.BOARDGAME)
            )
        )

        // Verify updated state - collecting from Flow again gets the new database state
        result = dao.observeCollection().first()
        assertEquals(2, result.size)
        assertTrue(result.none { it.gameId == 1L })
        assertTrue(result.all { it.gameId in listOf(2L, 3L) })
    }

    // --- Test 6: Delete operations ---

    @Test
    fun deleteAllRemovesAllItems() = runTest {
        // Insert items
        val items = listOf(
            createTestEntity(1, "Game 1", GameSubtype.BOARDGAME),
            createTestEntity(2, "Game 2", GameSubtype.BOARDGAME),
            createTestEntity(3, "Game 3", GameSubtype.BOARDGAME)
        )
        dao.insertAll(items)

        // Verify items exist
        var result = dao.getCollection()
        assertEquals(3, result.size)

        // Delete all
        dao.deleteAll()

        // Verify all deleted
        result = dao.getCollection()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteAllOnEmptyDatabaseDoesNotFail() = runTest {
        // Delete on empty database
        dao.deleteAll()

        // Verify still empty
        val result = dao.getCollection()
        assertTrue(result.isEmpty())
    }

    // --- Test 7: Observe collection count ---

    @Test
    fun observeCollectionCountReturnsCorrectCount() = runTest {
        // Initially should be 0
        var count = dao.observeCollectionCount().first()
        assertEquals(0L, count)

        // Insert items
        val items = listOf(
            createTestEntity(1, "Game 1", GameSubtype.BOARDGAME),
            createTestEntity(2, "Game 2", GameSubtype.BOARDGAME),
            createTestEntity(3, "Game 3", GameSubtype.BOARDGAME_EXPANSION)
        )
        dao.insertAll(items)

        // Verify count updated
        count = dao.observeCollectionCount().first()
        assertEquals(3L, count)
    }

    @Test
    fun observeCollectionCountUpdatesAfterDelete() = runTest {
        // Insert items
        dao.insertAll(
            listOf(
                createTestEntity(1, "Game 1", GameSubtype.BOARDGAME),
                createTestEntity(2, "Game 2", GameSubtype.BOARDGAME)
            )
        )

        // Verify initial count
        var count = dao.observeCollectionCount().first()
        assertEquals(2L, count)

        // Delete all
        dao.deleteAll()

        // Verify count updated
        count = dao.observeCollectionCount().first()
        assertEquals(0L, count)
    }

    // --- Test 8: Observe unplayed games count ---

    @Test
    fun observeUnplayedGamesCountReturnsCorrectCount() = runTest {
        // Insert collection items
        dao.insertAll(
            listOf(
                createTestEntity(1, "Game 1", GameSubtype.BOARDGAME),
                createTestEntity(2, "Game 2", GameSubtype.BOARDGAME),
                createTestEntity(3, "Game 3", GameSubtype.BOARDGAME)
            )
        )

        // Initially all games are unplayed
        var count = dao.observeUnplayedGamesCount().first()
        assertEquals(3L, count)

        // Insert a play for game 1
        val playDao = database.playDao()
        playDao.insert(
            app.meeplebook.core.database.entity.PlayEntity(
                id = 1,
                date = parseDateString("2024-01-01"),
                quantity = 1,
                length = 60,
                incomplete = false,
                location = null,
                gameId = 1,
                gameName = "Game 1",
                comments = null
            )
        )

        // Now only 2 games should be unplayed
        count = dao.observeUnplayedGamesCount().first()
        assertEquals(2L, count)
    }

    @Test
    fun observeUnplayedGamesCountReturnsZeroWhenAllPlayed() = runTest {
        // Insert collection items
        dao.insertAll(
            listOf(
                createTestEntity(1, "Game 1", GameSubtype.BOARDGAME),
                createTestEntity(2, "Game 2", GameSubtype.BOARDGAME)
            )
        )

        // Insert plays for all games
        val playDao = database.playDao()
        playDao.insertAll(
            listOf(
                app.meeplebook.core.database.entity.PlayEntity(
                    id = 1,
                    date = parseDateString("2024-01-01"),
                    quantity = 1,
                    length = 60,
                    incomplete = false,
                    location = null,
                    gameId = 1,
                    gameName = "Game 1",
                    comments = null
                ),
                app.meeplebook.core.database.entity.PlayEntity(
                    id = 2,
                    date = parseDateString("2024-01-02"),
                    quantity = 1,
                    length = 60,
                    incomplete = false,
                    location = null,
                    gameId = 2,
                    gameName = "Game 2",
                    comments = null
                )
            )
        )

        // All games are played
        val count = dao.observeUnplayedGamesCount().first()
        assertEquals(0L, count)
    }

    // --- Test 9: Observe most recently added item ---

    @Test
    fun observeMostRecentlyAddedItemReturnsLatestItem() = runTest {
        // Insert items with different lastModifiedDate
        val now = Instant.now()
        dao.insertAll(
            listOf(
                createTestEntity(1, "Oldest Game", GameSubtype.BOARDGAME, lastModifiedDate = now.minusSeconds(3600)),
                createTestEntity(2, "Middle Game", GameSubtype.BOARDGAME, lastModifiedDate = now.minusSeconds(1800)),
                createTestEntity(3, "Newest Game", GameSubtype.BOARDGAME, lastModifiedDate = now)
            )
        )

        // Verify most recent item
        val result = dao.observeMostRecentlyAddedItem().first()

        assertNotNull(result)
        assertEquals(3L, result?.gameId)
        assertEquals("Newest Game", result?.name)
    }

    @Test
    fun observeMostRecentlyAddedItemReturnsNullWhenEmpty() = runTest {
        // Observe with no items
        val result = dao.observeMostRecentlyAddedItem().first()

        assertEquals(null, result)
    }

    @Test
    fun observeMostRecentlyAddedItemIgnoresNullDates() = runTest {
        // Insert items, some with null lastModifiedDate
        val now = Instant.now()
        
        // Insert item with null date by using a modified entity
        database.compileStatement("INSERT INTO collection_items (gameId, subtype, name, yearPublished, thumbnail, lastModifiedDate) VALUES (1, 'BOARDGAME', 'No Date Game', 2020, null, null)").executeInsert()
        
        dao.insert(createTestEntity(2, "Game With Date", GameSubtype.BOARDGAME, lastModifiedDate = now))

        // Should return the game with a date
        val result = dao.observeMostRecentlyAddedItem().first()

        assertNotNull(result)
        assertEquals(2L, result?.gameId)
        assertEquals("Game With Date", result?.name)
    }

    // --- Test 10: Observe first unplayed game ---

    @Test
    fun observeFirstUnplayedGameReturnsFirstAlphabetically() = runTest {
        // Insert collection items in random order
        dao.insertAll(
            listOf(
                createTestEntity(3, "Zombicide", GameSubtype.BOARDGAME),
                createTestEntity(1, "Agricola", GameSubtype.BOARDGAME),
                createTestEntity(2, "Pandemic", GameSubtype.BOARDGAME)
            )
        )

        // Verify first unplayed game (alphabetically)
        val result = dao.observeFirstUnplayedGame().first()

        assertNotNull(result)
        assertEquals(1L, result?.gameId)
        assertEquals("Agricola", result?.name)
    }

    @Test
    fun observeFirstUnplayedGameSkipsPlayedGames() = runTest {
        // Insert collection items
        dao.insertAll(
            listOf(
                createTestEntity(1, "Agricola", GameSubtype.BOARDGAME),
                createTestEntity(2, "Brass", GameSubtype.BOARDGAME),
                createTestEntity(3, "Catan", GameSubtype.BOARDGAME)
            )
        )

        // Insert plays for first two games
        val playDao = database.playDao()
        playDao.insertAll(
            listOf(
                app.meeplebook.core.database.entity.PlayEntity(
                    id = 1,
                    date = parseDateString("2024-01-01"),
                    quantity = 1,
                    length = 60,
                    incomplete = false,
                    location = null,
                    gameId = 1,
                    gameName = "Agricola",
                    comments = null
                ),
                app.meeplebook.core.database.entity.PlayEntity(
                    id = 2,
                    date = parseDateString("2024-01-02"),
                    quantity = 1,
                    length = 60,
                    incomplete = false,
                    location = null,
                    gameId = 2,
                    gameName = "Brass",
                    comments = null
                )
            )
        )

        // Should return Catan (first unplayed)
        val result = dao.observeFirstUnplayedGame().first()

        assertNotNull(result)
        assertEquals(3L, result?.gameId)
        assertEquals("Catan", result?.name)
    }

    @Test
    fun observeFirstUnplayedGameReturnsNullWhenAllPlayed() = runTest {
        // Insert collection items
        dao.insertAll(
            listOf(
                createTestEntity(1, "Game 1", GameSubtype.BOARDGAME)
            )
        )

        // Insert play for the game
        val playDao = database.playDao()
        playDao.insert(
            app.meeplebook.core.database.entity.PlayEntity(
                id = 1,
                date = parseDateString("2024-01-01"),
                quantity = 1,
                length = 60,
                incomplete = false,
                location = null,
                gameId = 1,
                gameName = "Game 1",
                comments = null
            )
        )

        // Should return null (no unplayed games)
        val result = dao.observeFirstUnplayedGame().first()

        assertEquals(null, result)
    }

    @Test
    fun observeFirstUnplayedGameReturnsNullWhenEmpty() = runTest {
        // Observe with no items
        val result = dao.observeFirstUnplayedGame().first()

        assertEquals(null, result)
    }

    // --- Helper functions ---

    /**
     * Creates a test [CollectionItemEntity] with default or specified values.
     * Uses null defaults for optional parameters to make test intentions clear.
     * 
     * Note: Parameter order (gameId, name, subtype, ...) differs from constructor
     * for better test readability. Most common usage: createTestEntity(id, name, subtype)
     */
    private fun createTestEntity(
        gameId: Long,
        name: String = "Test Game $gameId",
        subtype: GameSubtype = GameSubtype.BOARDGAME,
        yearPublished: Int? = null,
        thumbnail: String? = null,
        lastModifiedDate: Instant = Instant.now()
    ): CollectionItemEntity {
        return CollectionItemEntity(
            gameId = gameId,
            subtype = subtype,
            name = name,
            yearPublished = yearPublished,
            thumbnail = thumbnail,
            lastModifiedDate = lastModifiedDate
        )
    }
}
