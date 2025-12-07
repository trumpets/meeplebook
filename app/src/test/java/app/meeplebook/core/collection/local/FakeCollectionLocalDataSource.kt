package app.meeplebook.core.collection.local

import app.meeplebook.core.collection.model.CollectionItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [CollectionLocalDataSource] for testing purposes.
 */
class FakeCollectionLocalDataSource : CollectionLocalDataSource {

    private val collection = MutableStateFlow<List<CollectionItem>>(emptyList())

    /** Tracks the number of times [saveCollection] was called. */
    var saveCollectionCallCount = 0
        private set

    /** Stores the last items passed to [saveCollection]. */
    var lastSaveItems: List<CollectionItem>? = null
        private set

    /** Tracks the number of times [clearCollection] was called. */
    var clearCollectionCallCount = 0
        private set

    override fun observeCollection(): Flow<List<CollectionItem>> {
        return collection
    }

    override suspend fun getCollection(): List<CollectionItem> {
        return collection.value
    }

    override suspend fun saveCollection(items: List<CollectionItem>) {
        saveCollectionCallCount++
        lastSaveItems = items
        collection.value = items
    }

    override suspend fun clearCollection() {
        clearCollectionCallCount++
        collection.value = emptyList()
    }

    /**
     * Sets the collection for a user directly for testing.
     */
    fun setCollection(items: List<CollectionItem>) {
        collection.value = items
    }
}
