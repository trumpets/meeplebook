package app.meeplebook.core.plays.local

import app.meeplebook.core.database.entity.PlayEntity
import app.meeplebook.core.database.entity.PlayerEntity
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
     * Saves (adds or updates) plays from remote.
     *
     * @param remotePlays The plays to save.
     */
    suspend fun saveRemotePlays(remotePlays: List<RemotePlayDto>)

    /**
     * Inserts a single play.
     *
     * @param playEntity The play entity to save.
     * @param playerEntities The player entities associated with the play.
     */
    suspend fun insertPlay(playEntity: PlayEntity, playerEntities: List<PlayerEntity>)

    /**
     * Clears all plays.
     */
    suspend fun clearPlays()

    /**
     * Retains only plays whose remote IDs are present in [remoteIds].
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
}
