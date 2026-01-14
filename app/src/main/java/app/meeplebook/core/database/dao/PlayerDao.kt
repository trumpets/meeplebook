package app.meeplebook.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.meeplebook.core.database.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for players.
 */
@Dao
interface PlayerDao {

    /**
     * Observe all players for a specific play.
     */
    @Query("SELECT * FROM players WHERE playId = :playId")
    fun observePlayersForPlay(playId: Long): Flow<List<PlayerEntity>>

    /**
     * Gets all players for a specific play.
     */
    @Query("SELECT * FROM players WHERE playId = :playId")
    suspend fun getPlayersForPlay(playId: Long): List<PlayerEntity>

    /**
     * Inserts a player, replacing on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity)

    /**
     * Inserts multiple players, replacing on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<PlayerEntity>)

    /**
     * Deletes all players for a specific play.
     */
    @Query("DELETE FROM players WHERE playId = :playId")
    suspend fun deletePlayersForPlay(playId: Long)

    /**
     * Deletes all players.
     */
    @Query("DELETE FROM players")
    suspend fun deleteAll()
}
