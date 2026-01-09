package app.meeplebook.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.meeplebook.core.database.entity.CollectionItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for collection items.
 */
@Dao
interface CollectionItemDao {

    /**
     * Observes all collection items.
     */
    @Query("SELECT * FROM collection_items ORDER BY name COLLATE NOCASE ASC")
    fun observeCollection(): Flow<List<CollectionItemEntity>>

    @Query("""
        SELECT * FROM collection_items
        WHERE name LIKE '%' || :nameQuery || '%'
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun observeCollectionByName(
        nameQuery: String
    ): Flow<List<CollectionItemEntity>>

    @Query("""
        SELECT * FROM collection_items
        WHERE NOT EXISTS (SELECT 1 FROM plays WHERE plays.gameId = collection_items.gameId)
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun observeCollectionUnplayed(): Flow<List<CollectionItemEntity>>

    /**
     * Gets all collection items.
     */
    @Query("SELECT * FROM collection_items ORDER BY name COLLATE NOCASE ASC")
    suspend fun getCollection(): List<CollectionItemEntity>

    /**
     * Inserts a collection item, replacing on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CollectionItemEntity)

    /**
     * Inserts multiple collection items, replacing on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CollectionItemEntity>)

    /**
     * Deletes all collection items.
     */
    @Query("DELETE FROM collection_items")
    suspend fun deleteAll()

    /**
     * Replaces the entire collection with new items.
     */
    @Transaction
    suspend fun replaceCollection(items: List<CollectionItemEntity>) {
        deleteAll()
        insertAll(items)
    }

    /**
     * Observe the count of items in the collection.
     */
    @Query("SELECT COUNT(*) FROM collection_items")
    fun observeCollectionCount(): Flow<Long>

    /**
     * Observe the count of unplayed games (games in collection that are not in plays table).
     */
    @Query("""
        SELECT COUNT(*) FROM collection_items 
        WHERE NOT EXISTS (SELECT 1 FROM plays WHERE plays.gameId = collection_items.gameId)
    """)
    fun observeUnplayedGamesCount(): Flow<Long>

    /**
     * Observe the collection item most recently added or updated on BGG (based on lastModifiedDate from BGG).
     */
    @Query("SELECT * FROM collection_items WHERE lastModifiedDate IS NOT NULL ORDER BY lastModifiedDate DESC LIMIT 1")
    fun observeMostRecentlyAddedItem(): Flow<CollectionItemEntity?>

    /**
     * Observe an unplayed game (first game in collection that has no plays).
     */
    @Query("""
        SELECT * FROM collection_items
        WHERE NOT EXISTS (SELECT 1 FROM plays WHERE plays.gameId = collection_items.gameId)
        ORDER BY name COLLATE NOCASE ASC
        LIMIT 1
    """)
    fun observeFirstUnplayedGame(): Flow<CollectionItemEntity?>
}
