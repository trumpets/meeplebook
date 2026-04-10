package app.meeplebook.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.meeplebook.core.database.entity.PlayerEntity
import app.meeplebook.core.database.projection.PlayerLocationProjection
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
     * Observe distinct non-null player colors used for the given game.
     *
     * @param gameId local id of the game
     * @return Flow emitting an ordered list of distinct color strings
     */
    @Query("""
        SELECT DISTINCT LOWER(player.color) AS color
        FROM players AS player
        INNER JOIN plays AS play
            ON player.playId = play.localId
        WHERE play.gameId = :gameId
          AND color IS NOT NULL
        ORDER BY color ASC
    """)
    fun observeColorsUsedForGame(gameId: Long): Flow<List<String>>

    /**
     * Searches distinct players whose name contains [query].
     *
     * @param query Substring to match against player names.
     * @return Flow emitting up to 20 matching distinct (name, username, userId) rows.
     */
    @Query("""
        SELECT 
            player.name AS name,
            player.username AS username,
            MAX(player.userId) AS userId
        FROM players AS player
        WHERE player.name LIKE '%' || :query || '%' COLLATE NOCASE
        GROUP BY player.name COLLATE NOCASE, player.username COLLATE NOCASE
        ORDER BY player.name ASC
        LIMIT 20
    """)
    fun searchDistinctPlayersByName(query: String): Flow<List<PlayerLocationProjection>>

    /**
     * Searches distinct players whose username contains [query].
     *
     * @param query Substring to match against usernames.
     * @return Flow emitting up to 20 matching distinct (name, username, userId) rows.
     */
    @Query("""
        SELECT 
            player.name AS name,
            player.username AS username,
            MAX(player.userId) AS userId
        FROM players AS player
        WHERE player.username LIKE '%' || :query || '%' COLLATE NOCASE
        GROUP BY player.name COLLATE NOCASE, player.username COLLATE NOCASE
        ORDER BY player.username ASC
        LIMIT 20
    """)
    fun searchDistinctPlayersByUsername(query: String): Flow<List<PlayerLocationProjection>>
}
