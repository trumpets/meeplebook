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
    fun observePlaysForGame(gameId: Int): Flow<List<Play>>

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
    suspend fun getPlaysForGame(gameId: Int): List<Play>

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
     * Gets the total count of plays (sum of quantities).
     */
    suspend fun getTotalPlaysCount(): Int

    /**
     * Gets the count of plays for a specific month.
     */
    suspend fun getPlaysCountForMonth(start: Instant, end: Instant): Int

    /**
     * Gets the most recent plays with a limit.
     */
    suspend fun getRecentPlays(limit: Int): List<Play>
}
