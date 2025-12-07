package app.meeplebook.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for collection items.
 */
@Dao
interface CollectionItemDao {

    /**
     * Observes all collection items for a specific user.
     */
    @Query("SELECT * FROM collection_items ORDER BY name ASC")
    fun observeCollection(): Flow<List<CollectionItemEntity>>

    /**
     * Gets all collection items for a specific user.
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
     * Deletes all collection items for a specific user.
     */
    @Query("DELETE FROM collection_items")
    suspend fun deleteAll()

    /**
     * Replaces the entire collection for a user with new items.
     */
    @Transaction
    suspend fun replaceCollection(items: List<CollectionItemEntity>) {
        deleteAll()
        insertAll(items)
    }
}
