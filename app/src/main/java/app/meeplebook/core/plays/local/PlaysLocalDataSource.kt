package app.meeplebook.core.plays.local

import app.meeplebook.core.database.entity.PlayEntity
import app.meeplebook.core.database.entity.PlayerEntity
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.remote.dto.RemotePlayDto
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local data source for storing and retrieving user plays.
 */
interface PlaysLocalDataSource {

    /**
     * Observes all plays.
     *
     * @return Flow emitting the user's plays.
     */
    fun observePlays(): Flow<List<Play>>

    /**
     * Observes all plays filtered by game name or location.
     *
     * @param gameNameOrLocationQuery Query string to match against game name or location.
     * @return Flow emitting the user's plays filtered by game name or location.
     */
    fun observePlaysByGameNameOrLocation(gameNameOrLocationQuery: String): Flow<List<Play>>

    /**
     * Observes all plays for a specific game.
     *
     * @return Flow emitting the user's plays for a specific game.
     */
    fun observePlaysForGame(gameId: Long): Flow<List<Play>>

    /**
     * Gets all plays.
     *
     * @return The user's plays.
     */
    suspend fun getPlays(): List<Play>

    /**
     * Gets all plays for a specific game.
     *
     * @return The user's plays.
     */
    suspend fun getPlaysForGame(gameId: Long): List<Play>

    /**
     * Saves pulled plays from the remote source into local storage.
     *
     * This is the local persistence half of the remote **pull sync** path. Future pending-play push
     * support will use separate outbox-oriented operations rather than overloading this method.
     *
     * @param remotePlays The plays to save.
     */
    suspend fun saveRemotePlays(remotePlays: List<RemotePlayDto>)

    /**
     * Returns local plays that should be retried by the outbox upload path.
     */
    suspend fun getPendingOrFailedPlays(): List<Play>

    /**
     * Inserts a single locally created play.
     *
     * This is the local-write half of the outbox pattern. The inserted play remains available for a
     * later background upload based on its sync status.
     *
     * @param playEntity The play entity to save.
     * @param playerEntities The player entities associated with the play.
     */
    suspend fun insertPlay(playEntity: PlayEntity, playerEntities: List<PlayerEntity>)

    /**
     * Marks a locally stored play as synced and optionally assigns the remote id returned by BGG.
     */
    suspend fun markPlayAsSynced(localPlayId: Long, remotePlayId: Long)

    /**
     * Marks a locally stored play as failed so it will be retried later.
     */
    suspend fun markPlayAsFailed(localPlayId: Long)

    /**
     * Clears all plays.
     */
    suspend fun clearPlays()

    /**
     * Retains only remote-backed plays whose remote IDs are present in [remoteIds].
     *
     * This supports reconciliation after remote **pull sync** and must not delete local outbox plays
     * that have no remote id yet.
     *
     * @param remoteIds List of remote play IDs that should be retained locally.
     */
    suspend fun retainByRemoteIds(remoteIds: List<Long>)

    /**
     * Observes the total count of plays (sum of quantities).
     */
    fun observeTotalPlaysCount(): Flow<Long>

    /**
     * Observes the count of plays for a specific month.
     */
    fun observePlaysCountForMonth(start: Instant, end: Instant): Flow<Long>

    /**
     * Observes the most recent plays with a limit.
     */
    fun observeRecentPlays(limit: Int): Flow<List<Play>>

    /**
     * Observes the count of unique games that have been played.
     */
    fun observeUniqueGamesCount(): Flow<Long>

    /**
     * Observes the locations where plays occurred matching the query.
     */
    fun observeLocations(query: String): Flow<List<String>>

    /**
     * Observes the most recent locations where plays occurred.
     */
    fun observeRecentLocations(): Flow<List<String>>

    /**
     * Observes players who have played at a specific location,
     * ordered by the number of plays at that location (desc).
     */
    fun observePlayersByLocation(location: String): Flow<List<PlayerIdentity>>

    /**
     * Observe distinct non-null player colors used for the given game.
     *
     * @param gameId local id of the game
     * @return Flow emitting an ordered list of distinct color strings
     */
    fun observeColorsUsedForGame(gameId: Long): Flow<List<String>>

    /**
     * Searches distinct players whose name contains [query].
     */
    fun searchPlayersByName(query: String): Flow<List<PlayerIdentity>>

    /**
     * Searches distinct players whose username contains [query].
     */
    fun searchPlayersByUsername(query: String): Flow<List<PlayerIdentity>>
}
