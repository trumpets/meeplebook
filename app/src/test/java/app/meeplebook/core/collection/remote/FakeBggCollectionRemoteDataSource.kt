package app.meeplebook.core.collection.remote

import app.meeplebook.core.collection.model.CollectionItem

/**
 * Fake implementation of [BggCollectionRemoteDataSource] for testing.
 */
class FakeBggCollectionRemoteDataSource : BggCollectionRemoteDataSource {

    var collectionResult: List<CollectionItem>? = null
    var exception: Exception? = null
    var getCollectionCallCount = 0
        private set
    var lastUsername: String? = null
        private set

    override suspend fun getCollection(username: String): List<CollectionItem> {
        getCollectionCallCount++
        lastUsername = username

        exception?.let { throw it }
        return collectionResult
            ?: throw IllegalStateException("FakeBggCollectionRemoteDataSource not configured")
    }
}
