package app.meeplebook.core.plays

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
     * Syncs all plays for a specific user from BGG.
     *
     * Fetches all pages of plays from BGG and stores them locally.
     * This method orchestrates multi-page fetching and merges results.
     *
     * @param username The BGG username.
     * @return Success with all the plays, or Failure with an error.
     */
    suspend fun syncPlays(username: String): AppResult<List<Play>, PlayError>

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
}
