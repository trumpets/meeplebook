package app.meeplebook.core.plays.local

import app.meeplebook.core.plays.model.Play
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
     * Saves (adds or updates) plays.
     *
     * @param plays The plays to save.
     */
    suspend fun savePlays(plays: List<Play>)

    /**
     * Saves (adds or updates) a single play.
     *
     * @param play The play to save.
     */
    suspend fun savePlay(play: Play)

    /**
     * Clears all plays.
     */
    suspend fun clearPlays()

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
}
