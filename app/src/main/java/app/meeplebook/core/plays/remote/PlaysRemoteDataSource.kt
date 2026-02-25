package app.meeplebook.core.plays.remote

import app.meeplebook.core.plays.remote.dto.RemotePlayDto

/**
 * Remote data source for fetching user plays from BGG.
 */
interface PlaysRemoteDataSource {
    /**
     * Fetches a user's plays from BGG.
     *
     * This method handles:
     * - 202 responses by retrying with exponential backoff
     * - Rate limiting by waiting between requests
     * - Pagination (100 records per page)
     *
     * @param username The BGG username.
     * @param page The page number to fetch (optional, starts at 1).
     * @return List of [RemotePlayDto]s from the specified page.
     * @throws app.meeplebook.core.network.RetryException if the fetch fails after retries.
     * @throws IllegalArgumentException if the username is blank.
     * @throws java.io.IOException for network-related errors.
     * @throws PlaysFetchException for unknown http status codes
     */
    suspend fun fetchPlays(username: String, page: Int? = null): List<RemotePlayDto>
}
