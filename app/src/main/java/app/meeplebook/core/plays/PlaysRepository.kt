package app.meeplebook.core.plays

import app.meeplebook.core.plays.domain.CreatePlayCommand
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Repository for managing user plays.
 */
interface PlaysRepository {

    /**
     * Observes plays from local storage.
     *
     * @return Flow emitting the user's plays.
     */
    fun observePlays(gameNameOrLocationQuery: String? = null): Flow<List<Play>>

    /**
     * Observes all plays for a specific game from local storage.
     *
     * @return Flow emitting the user's plays for a specific game.
     */
    fun observePlaysForGame(gameId: Long): Flow<List<Play>>

    /**
     * Gets plays from local storage.
     *
     * @return The user's plays.
     */
    suspend fun getPlays(): List<Play>

    /**
     * Gets all plays for a specific game from local storage.
     *
     * @return The user's plays.
     */
    suspend fun getPlaysForGame(gameId: Long): List<Play>

    /**
     * Performs the repository-owned **pull sync** of all remote plays for a specific user.
     *
     * Fetches all pages of plays from BGG and stores them locally.
     * This method owns remote paging, local persistence, and remote/local reconciliation for pulled
     * plays only. Higher-level orchestration concerns such as auth gating, sync-state updates,
     * worker execution, and future pending-play push sequencing live outside the repository.
     *
     * @param username The BGG username.
     * @return Success, or Failure with an error.
     */
    suspend fun syncPlays(username: String): AppResult<Unit, PlayError>

    /**
     * Uploads locally queued plays whose sync status is [app.meeplebook.core.plays.model.PlaySyncStatus.PENDING]
     * or [app.meeplebook.core.plays.model.PlaySyncStatus.FAILED].
     *
     * Uploads happen sequentially so each play can independently transition to `SYNCED` or `FAILED`.
     * Fatal transport/auth failures stop the batch and return a failure; per-play validation failures
     * are persisted as `FAILED` and the repository continues to later plays.
     */
    suspend fun syncPendingPlays(): AppResult<Unit, PlayError>

    /**
     * Creates a new play locally as part of the outbox flow.
     *
     * The created play is persisted immediately and marked with a non-synced status so background
     * sync can upload it later.
     *
     * @param command The command containing the details of the play to create.
     */
    suspend fun createPlay(command: CreatePlayCommand)

    /**
     * Clears all plays from local storage.
     */
    suspend fun clearPlays()

    /**
     * Observes the total count of plays (sum of quantities).
     */
    fun observeTotalPlaysCount(): Flow<Long>

    /**
     * Observes the count of plays for a specific period.
     */
    fun observePlaysCountForPeriod(start: Instant, end: Instant): Flow<Long>

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
