package app.meeplebook.core.plays.local

import app.meeplebook.core.plays.model.Play
import kotlinx.coroutines.flow.Flow

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
     * Gets all plays.
     *
     * @return The user's plays.
     */
    suspend fun getPlays(): List<Play>

    /**
     * Saves (adds or updates) plays.
     *
     * @param plays The plays to save.
     */
    suspend fun savePlays(plays: List<Play>)

    /**
     * Clears all plays.
     */
    suspend fun clearPlays()
}
