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
    fun observePlayWithPlayersById(playId: Int): Flow<PlayWithPlayers>

    /**
     * Gets a specific play with its players by ID.
     */
    @Transaction
    @Query("SELECT * FROM plays WHERE id = :playId")
    suspend fun getPlayWithPlayersById(playId: Int): PlayWithPlayers?

    /**
     * Observes plays with their players for a specific game.
     */
    @Transaction
    @Query("SELECT * FROM plays WHERE gameId = :gameId")
    fun observePlaysWithPlayersForGame(gameId: Int): Flow<List<PlayWithPlayers>>

    /**
     * Gets plays with their players for a specific game.
     */
    @Transaction
    @Query("SELECT * FROM plays WHERE gameId = :gameId")
    suspend fun getPlaysWithPlayersForGame(gameId: Int): List<PlayWithPlayers>

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
     * Observes all plays for a specific game, ordered by date descending.
     */
    @Query("SELECT * FROM plays WHERE gameId = :gameId ORDER BY date DESC")
    fun observePlaysForGame(gameId: Int): Flow<List<PlayEntity>>

    /**
     * Gets all plays for a specific game, ordered by date descending.
     */
    @Query("SELECT * FROM plays WHERE gameId = :gameId ORDER BY date DESC")
    suspend fun getPlaysForGame(gameId: Int): List<PlayEntity>

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
     * Gets the total count of plays (sum of quantities).
     */
    @Query("SELECT COALESCE(SUM(quantity), 0) FROM plays")
    suspend fun getTotalPlaysCount(): Long

    /**
     * Gets the count of plays for a specific month.
     */
    @Query("SELECT COALESCE(SUM(quantity), 0) FROM plays WHERE date >= :start AND date < :end")
    suspend fun getPlaysCountForMonth(start: Instant, end: Instant): Long

    /**
     * Gets the most recent plays with a limit, ordered by date descending.
     */
    @Transaction
    @Query("SELECT * FROM plays ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentPlaysWithPlayers(limit: Int): List<PlayWithPlayers>
}
