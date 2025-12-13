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
    @Query("SELECT * FROM collection_items ORDER BY name ASC")
    fun observeCollection(): Flow<List<CollectionItemEntity>>

    /**
     * Gets all collection items.
     */
    @Query("SELECT * FROM collection_items ORDER BY name ASC")
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
     * Gets the count of items in the collection.
     */
    @Query("SELECT COUNT(*) FROM collection_items")
    suspend fun getCollectionCount(): Int

    /**
     * Gets the count of unplayed games (games in collection that are not in plays table).
     */
    @Query("""
        SELECT COUNT(*) FROM collection_items 
        WHERE gameId NOT IN (SELECT DISTINCT gameId FROM plays)
    """)
    suspend fun getUnplayedGamesCount(): Int

    /**
     * Gets the collection item with the most recent lastModified date.
     */
    @Query("SELECT * FROM collection_items WHERE lastModifiedDate IS NOT NULL ORDER BY lastModifiedDate DESC LIMIT 1")
    suspend fun getMostRecentlyAddedItem(): CollectionItemEntity?

    /**
     * Gets an unplayed game (first game in collection that has no plays).
     */
    @Query("""
        SELECT * FROM collection_items 
        WHERE gameId NOT IN (SELECT DISTINCT gameId FROM plays) 
        LIMIT 1
    """)
    suspend fun getFirstUnplayedGame(): CollectionItemEntity?
}
