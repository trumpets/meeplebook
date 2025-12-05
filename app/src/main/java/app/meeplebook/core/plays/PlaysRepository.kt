package app.meeplebook.core.plays

import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlaysError
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user's board game plays.
 */
interface PlaysRepository {

    /**
     * Fetches plays for a user with pagination.
     *
     * @param username BGG username
     * @param page Page number (1-indexed)
     * @return Result containing plays response or error
     */
    suspend fun getPlays(username: String, page: Int = 1): AppResult<PlaysResponse, PlaysError>

    /**
     * Observes the cached plays.
     * Emits empty list when no cached data is available.
     */
    fun observePlays(): Flow<List<Play>>
}

/**
 * Response data for plays fetch.
 */
data class PlaysResponse(
    val plays: List<Play>,
    val totalPlays: Int,
    val currentPage: Int,
    val hasMorePages: Boolean
)
