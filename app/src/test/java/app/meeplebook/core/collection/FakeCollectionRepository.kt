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
    var syncResult: AppResult<List<CollectionItem>, CollectionError>? = null

    /**
     * Tracks the number of times [syncCollection] was called.
     */
    var syncCallCount = 0
        private set

    /**
     * Tracks the number of times [clearCollection] was called.
     */
    var clearCallCount = 0
        private set

    override fun observeCollection(): Flow<List<CollectionItem>> {
        return collection
    }

    override suspend fun getCollection(): List<CollectionItem> {
        return collection.value
    }

    override suspend fun syncCollection(username: String): AppResult<List<CollectionItem>, CollectionError> {
        syncCallCount++

        val result = syncResult ?: throw IllegalStateException("FakeCollectionRepository not configured")

        if (result is AppResult.Success) {
            collection.value = result.data
        }

        return result
    }

    override suspend fun clearCollection() {
        clearCallCount++
        collection.value = emptyList()
    }

    /**
     * Sets the collection directly for testing.
     */
    fun setCollection(items: List<CollectionItem>) {
        collection.value = items
    }
}
