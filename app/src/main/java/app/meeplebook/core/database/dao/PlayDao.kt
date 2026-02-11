package app.meeplebook.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
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
           OR (location IS NOT NULL AND location LIKE '%' || :gameNameOrLocationQuery || '%')
        ORDER BY date DESC
    """)
    // TODO: this query should perform case insensitive search
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
    @Query("SELECT * FROM plays WHERE localId = :localPlayId")
    fun observePlayWithPlayersById(localPlayId: Long): Flow<PlayWithPlayers>

    /**
     * Gets a specific play with its players by ID.
     */
    @Transaction
    @Query("SELECT * FROM plays WHERE localId = :localPlayId")
    suspend fun getPlayWithPlayersById(localPlayId: Long): PlayWithPlayers?

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
     * Gets all plays that have a remote ID.
     */
    @Query("SELECT * FROM plays WHERE remoteId IS NOT NULL ORDER BY date DESC")
    suspend fun getRemotePlays(): List<PlayEntity>

    /**
     * Gets a specific play by ID.
     */
    @Query("SELECT * FROM plays WHERE localId = :localPlayId")
    suspend fun getPlayById(localPlayId: Long): PlayEntity?

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
     * Gets plays by their remote IDs.
     */
    @Query("SELECT * FROM plays WHERE remoteId IN (:remoteIds)")
    suspend fun getByRemoteIds(remoteIds: List<Long>): List<PlayEntity>

    /**
     * Inserts a play, replacing on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(play: PlayEntity): Long

    /**
     * Inserts multiple plays, replacing on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plays: List<PlayEntity>): List<Long>

    /**
     * Upserts multiple plays.
     */
    @Upsert
    suspend fun upsertAll(plays: List<PlayEntity>)

    /**
     * Deletes all plays.
     */
    @Query("DELETE FROM plays")
    suspend fun deleteAll()

    /**
     * Deletes plays by their remote IDs.
     */
    @Query("DELETE FROM plays WHERE remoteId IN (:remoteIds)")
    suspend fun deleteByRemoteIds(remoteIds: List<Long>)

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

    /**
     * Searches for unique play locations that start with the given query, case-insensitively,
     * ordered alphabetically, limited to 10 results.
     */
    @Query("""
        SELECT DISTINCT location
        FROM plays
        WHERE location IS NOT NULL
        AND location LIKE :query || '%'
        ORDER BY location COLLATE NOCASE ASC
        LIMIT 10
    """)
    fun observeLocations(query: String): Flow<List<String>>

    /**
     * Observes the 10 most recent unique play locations, ordered by the most recent play date.
     */
    @Query("""
        SELECT DISTINCT location
        FROM plays
        WHERE location IS NOT NULL
        ORDER BY date DESC
        LIMIT 10
    """)
    fun observeRecentLocations(): Flow<List<String>>
}
