package app.meeplebook.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for plays.
 */
@Dao
interface PlayDao {

    /**
     * Observes all plays.
     */
    @Query("SELECT * FROM plays ORDER BY date DESC")
    fun observePlays(): Flow<List<PlayEntity>>

    /**
     * Gets all plays.
     */
    @Query("SELECT * FROM plays ORDER BY date DESC")
    suspend fun getPlays(): List<PlayEntity>

    /**
     * Gets a specific play by ID.
     */
    @Query("SELECT * FROM plays WHERE id = :playId")
    suspend fun getPlayById(playId: Int): PlayEntity?

    /**
     * Inserts a play, replacing on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(play: PlayEntity)

    /**
     * Inserts multiple plays, replacing on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plays: List<PlayEntity>)

    /**
     * Deletes all plays.
     */
    @Query("DELETE FROM plays")
    suspend fun deleteAll()

    /**
     * Replaces all plays with new plays.
     */
    @Transaction
    suspend fun replacePlays(plays: List<PlayEntity>) {
        deleteAll()
        insertAll(plays)
    }
}
