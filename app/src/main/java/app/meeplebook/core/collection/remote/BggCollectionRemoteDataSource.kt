package app.meeplebook.core.collection.remote

import app.meeplebook.core.collection.model.CollectionItem

/**
 * Remote data source interface for fetching BGG collection.
 */
interface BggCollectionRemoteDataSource {

    /**
     * Fetches the collection for a user.
     *
     * @param username BGG username
     * @return List of collection items
     * @throws CollectionNotReadyException if BGG returns 202 after max retries
     * @throws java.io.IOException if network error occurs
     */
    suspend fun getCollection(username: String): List<CollectionItem>
}

/**
 * Exception thrown when BGG collection is not ready after max retries.
 */
class CollectionNotReadyException(message: String) : Exception(message)
