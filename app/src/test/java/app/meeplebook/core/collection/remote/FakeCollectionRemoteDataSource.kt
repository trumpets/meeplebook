package app.meeplebook.core.collection.remote

import app.meeplebook.core.collection.model.CollectionItem

/**
 * Fake implementation of [CollectionRemoteDataSource] for testing purposes.
 */
class FakeCollectionRemoteDataSource : CollectionRemoteDataSource {

    /**
     * Configure this to control the result of [fetchCollection] calls.
     * If null, fetchCollection will throw the configured exception.
     */
    var fetchResult: List<CollectionItem>? = null

    /**
     * Configure this exception to be thrown on [fetchCollection] calls.
     */
    var fetchException: Exception? = null

    /**
     * Tracks the number of times [fetchCollection] was called.
     */
    var fetchCallCount = 0
        private set

    /**
     * Stores the last username passed to [fetchCollection].
     */
    var lastFetchUsername: String? = null
        private set

    override suspend fun fetchCollection(username: String): List<CollectionItem> {
        fetchCallCount++
        lastFetchUsername = username

        fetchException?.let { throw it }
        return fetchResult ?: throw IllegalStateException("FakeCollectionRemoteDataSource not configured")
    }
}
