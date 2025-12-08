package app.meeplebook.core.plays

import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing user plays.
 */
interface PlaysRepository {

    /**
     * Observes plays from local storage.
     *
     * @return Flow emitting the user's plays.
     */
    fun observePlays(): Flow<List<Play>>

    /**
     * Gets plays from local storage.
     *
     * @return The user's plays.
     */
    suspend fun getPlays(): List<Play>

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
}
