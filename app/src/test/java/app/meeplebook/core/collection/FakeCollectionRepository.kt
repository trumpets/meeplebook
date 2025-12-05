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

    private val collections = mutableMapOf<String, MutableStateFlow<List<CollectionItem>>>()

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
     * Stores the last username passed to [syncCollection].
     */
    var lastSyncUsername: String? = null
        private set

    /**
     * Tracks the number of times [clearCollection] was called.
     */
    var clearCallCount = 0
        private set

    override fun observeCollection(username: String): Flow<List<CollectionItem>> {
        return getOrCreateFlow(username)
    }

    override suspend fun getCollection(username: String): List<CollectionItem> {
        return collections[username]?.value ?: emptyList()
    }

    override suspend fun syncCollection(username: String): AppResult<List<CollectionItem>, CollectionError> {
        syncCallCount++
        lastSyncUsername = username

        val result = syncResult ?: throw IllegalStateException("FakeCollectionRepository not configured")

        if (result is AppResult.Success) {
            getOrCreateFlow(username).value = result.data
        }

        return result
    }

    override suspend fun clearCollection(username: String) {
        clearCallCount++
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
