package app.meeplebook.core.collection

import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [CollectionRepository] for testing purposes.
 */
class FakeCollectionRepository : CollectionRepository {

    private val collection = MutableStateFlow<List<CollectionItem>>(emptyList())

    /**
     * Configure this to control the result of [syncCollection] calls.
     */
    var syncCollectionResult: AppResult<List<CollectionItem>, CollectionError> = 
        AppResult.Failure(CollectionError.Unknown(IllegalStateException("FakeCollectionRepository not configured")))

    /**
     * Tracks the number of times [syncCollection] was called.
     */
    var syncCollectionCallCount = 0
        private set

    /**
     * Stores the last username passed to [syncCollection].
     */
    var lastSyncUsername: String? = null
        private set

    override fun observeCollection(): Flow<List<CollectionItem>> = collection

    override suspend fun getCollection(): List<CollectionItem> {
        return collection.value
    }

    override suspend fun syncCollection(username: String): AppResult<List<CollectionItem>, CollectionError> {
        syncCollectionCallCount++
        lastSyncUsername = username

        when (val result = syncCollectionResult) {
            is AppResult.Success -> collection.value = result.data
            is AppResult.Failure -> { /* no-op */ }
        }

        return syncCollectionResult
    }

    override suspend fun clearCollection() {
        collection.value = emptyList()
    }

    /**
     * Sets the collection directly for testing.
     */
    fun setCollection(items: List<CollectionItem>) {
        collection.value = items
    }
}
