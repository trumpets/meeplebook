package app.meeplebook.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.meeplebook.core.database.entity.PlayEntity
import app.meeplebook.core.database.entity.PlayWithPlayers
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Data Access Object for plays.
 */
@Dao
interface PlayDao {

    /**
     * Observes all plays with their players.
     */
    @Transaction
    @Query("SELECT * FROM plays ORDER BY date DESC")
    fun observePlaysWithPlayers(): Flow<List<PlayWithPlayers>>

    @Transaction
    @Query("""
        SELECT * FROM plays
        WHERE gameName LIKE '%' || :gameNameOrLocationQuery || '%'
           OR location LIKE '%' || :gameNameOrLocationQuery || '%'
        ORDER BY date DESC
    """)
    fun observePlaysWithPlayersByGameNameOrLocation(
        gameNameOrLocationQuery: String
    ): Flow<List<PlayWithPlayers>>

    /**
     * Gets all plays with their players.
     */
    @Transaction
    @Query("SELECT * FROM plays ORDER BY date DESC")
    suspend fun getPlaysWithPlayers(): List<PlayWithPlayers>

    /**
     * Observes a specific play with its players by ID.
     */
    @Transaction
    @Query("SELECT * FROM plays WHERE id = :playId")
    fun observePlayWithPlayersById(playId: Long): Flow<PlayWithPlayers>

    /**
     * Gets a specific play with its players by ID.
     */
    @Transaction
    @Query("SELECT * FROM plays WHERE id = :playId")
    suspend fun getPlayWithPlayersById(playId: Long): PlayWithPlayers?

    /**
     * Observes plays with their players for a specific game.
     */
    @Transaction
    @Query("SELECT * FROM plays WHERE gameId = :gameId")
    fun observePlaysWithPlayersForGame(gameId: Long): Flow<List<PlayWithPlayers>>

    /**
     * Gets plays with their players for a specific game.
     */
    @Transaction
    @Query("SELECT * FROM plays WHERE gameId = :gameId")
    suspend fun getPlaysWithPlayersForGame(gameId: Long): List<PlayWithPlayers>

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
    suspend fun getPlayById(playId: Long): PlayEntity?

    /**
     * Observes all plays for a specific game, ordered by date descending.
     */
    @Query("SELECT * FROM plays WHERE gameId = :gameId ORDER BY date DESC")
    fun observePlaysForGame(gameId: Long): Flow<List<PlayEntity>>

    /**
     * Gets all plays for a specific game, ordered by date descending.
     */
    @Query("SELECT * FROM plays WHERE gameId = :gameId ORDER BY date DESC")
    suspend fun getPlaysForGame(gameId: Long): List<PlayEntity>

    /**
     * Observes the count of unique games that have been played.
     */
    @Query("SELECT COUNT(DISTINCT gameId) FROM plays")
    fun observeUniqueGamesCount(): Flow<Long>

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
     * Observes the total count of plays (sum of quantities).
     */
    @Query("SELECT COALESCE(SUM(quantity), 0) FROM plays")
    fun observeTotalPlaysCount(): Flow<Long>

    /**
     * Observes the count of plays for a specific month.
     */
    @Query("SELECT COALESCE(SUM(quantity), 0) FROM plays WHERE date >= :start AND date < :end")
    fun observePlaysCountForMonth(start: Instant, end: Instant): Flow<Long>

    /**
     * Observes the most recent plays with a limit, ordered by date descending.
     */
    @Transaction
    @Query("SELECT * FROM plays ORDER BY date DESC LIMIT :limit")
    fun observeRecentPlaysWithPlayers(limit: Int): Flow<List<PlayWithPlayers>>
}
