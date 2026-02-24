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
     * Deletes all players for the specified list of play IDs.
     */
    @Query("DELETE FROM players WHERE playId IN (:localPlayIds)")
    suspend fun deletePlayersForPlays(localPlayIds: List<Long>)

    /**
     * Deletes all players.
     */
    @Query("DELETE FROM players")
    suspend fun deleteAll()

    /**
     * Observes distinct colors used by players in plays of a specific game.
     *
     * @param gameId The game ID to filter plays by.
     * @return Flow emitting a list of distinct, non-null colors ordered alphabetically.
     */
    @Query("""
        SELECT DISTINCT players.color
        FROM players
        INNER JOIN plays ON players.playId = plays.localId
        WHERE plays.gameId = :gameId
        AND players.color IS NOT NULL
        ORDER BY players.color ASC
    """)
    fun observeColorsUsedForGame(gameId: Long): Flow<List<String>>
}
