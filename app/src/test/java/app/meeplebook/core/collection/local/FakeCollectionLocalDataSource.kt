package app.meeplebook.core.collection.local

import app.meeplebook.core.collection.model.CollectionItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [CollectionLocalDataSource] for testing purposes.
 */
class FakeCollectionLocalDataSource : CollectionLocalDataSource {

    private val collections = mutableMapOf<String, MutableStateFlow<List<CollectionItem>>>()

    /** Tracks the number of times [saveCollection] was called. */
    var saveCollectionCallCount = 0
        private set

    /** Stores the last username passed to [saveCollection]. */
    var lastSaveUsername: String? = null
        private set

    /** Stores the last items passed to [saveCollection]. */
    var lastSaveItems: List<CollectionItem>? = null
        private set

    /** Tracks the number of times [clearCollection] was called. */
    var clearCollectionCallCount = 0
        private set

    override fun observeCollection(username: String): Flow<List<CollectionItem>> {
        return getOrCreateFlow(username)
    }

    override suspend fun getCollection(username: String): List<CollectionItem> {
        return collections[username]?.value ?: emptyList()
    }

    override suspend fun saveCollection(username: String, items: List<CollectionItem>) {
        saveCollectionCallCount++
        lastSaveUsername = username
        lastSaveItems = items
        getOrCreateFlow(username).value = items
    }

    override suspend fun clearCollection(username: String) {
        clearCollectionCallCount++
        collections[username]?.value = emptyList()
    }

    /**
     * Sets the collection for a user directly for testing.
     */
    fun setCollection(username: String, items: List<CollectionItem>) {
        getOrCreateFlow(username).value = items
    }

    private fun getOrCreateFlow(username: String): MutableStateFlow<List<CollectionItem>> {
        return collections.getOrPut(username) { MutableStateFlow(emptyList()) }
    }
}
