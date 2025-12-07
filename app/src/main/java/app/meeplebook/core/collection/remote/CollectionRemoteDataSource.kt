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
     * @throws CollectionFetchException if the fetch fails after retries.
     */
    suspend fun fetchCollection(username: String): List<CollectionItem>
}
