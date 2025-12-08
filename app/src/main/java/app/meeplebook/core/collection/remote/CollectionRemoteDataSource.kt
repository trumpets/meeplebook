package app.meeplebook.core.collection.remote

import app.meeplebook.core.collection.model.CollectionItem

/**
 * Remote data source for fetching user collections from BGG.
 */
interface CollectionRemoteDataSource {
    /**
     * Fetches a user's collection from BGG.
     *
     * This method handles:
     * - 202 responses by retrying with exponential backoff
     * - Rate limiting by waiting between requests
     * - Fetching both boardgames and expansions separately
     *
     * @param username The BGG username.
     * @return List of [CollectionItem]s in the user's collection.
     * @throws app.meeplebook.core.network.RetryException if the fetch fails after retries.
     * @throws IllegalArgumentException if the username is blank.
     * @throws java.io.IOException for network-related errors.
     */
    suspend fun fetchCollection(username: String): List<CollectionItem>
}
