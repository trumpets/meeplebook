package app.meeplebook.core.collection

import app.meeplebook.core.collection.model.CollectionDataQuery
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [CollectionRepository] for testing purposes.
 */
class FakeCollectionRepository : CollectionRepository {

    private val _collection = MutableStateFlow<List<CollectionItem>>(emptyList())
    private val _collectionCount = MutableStateFlow(0L)
    private val _unplayedCount = MutableStateFlow(0L)
    private val _mostRecentlyAdded = MutableStateFlow<CollectionItem?>(null)
    private val _firstUnplayed = MutableStateFlow<CollectionItem?>(null)

    var syncCollectionResult: AppResult<List<CollectionItem>, CollectionError> =
        AppResult.Failure(CollectionError.Unknown(IllegalStateException("FakeCollectionRepository not configured")))

    var syncCallCount = 0
        private set

    var lastSyncUsername: String? = null
        private set

    var lastObserveCollectionQuery: CollectionDataQuery? = null
        private set

    override fun observeCollection(query: CollectionDataQuery?): Flow<List<CollectionItem>> {
        lastObserveCollectionQuery = query
        return _collection
    }

    override suspend fun getCollection(): List<CollectionItem> = _collection.value

    override suspend fun syncCollection(username: String): AppResult<List<CollectionItem>, CollectionError> {
        syncCallCount++
        lastSyncUsername = username

        when (val result = syncCollectionResult) {
            is AppResult.Success -> {
                _collection.value = result.data
                updateComputedValues(result.data)
            }
            is AppResult.Failure -> { /* no-op */ }
        }

        return syncCollectionResult
    }

    override suspend fun clearCollection() {
        _collection.value = emptyList()
        _collectionCount.value = 0L
        _unplayedCount.value = 0L
        _mostRecentlyAdded.value = null
        _firstUnplayed.value = null
    }

    override fun observeCollectionCount(): Flow<Long> = _collectionCount

    override fun observeUnplayedGamesCount(): Flow<Long> = _unplayedCount

    override fun observeMostRecentlyAddedItem(): Flow<CollectionItem?> = _mostRecentlyAdded

    override fun observeFirstUnplayedGame(): Flow<CollectionItem?> = _firstUnplayed

    /**
     * Sets the collection directly for testing purposes.
     */
    fun setCollection(items: List<CollectionItem>) {
        _collection.value = items
        updateComputedValues(items)
    }

    /**
     * Sets the collection count directly for testing purposes.
     */
    fun setCollectionCount(count: Long) {
        _collectionCount.value = count
    }

    /**
     * Sets the unplayed count directly for testing purposes.
     */
    fun setUnplayedCount(count: Long) {
        _unplayedCount.value = count
    }

    /**
     * Sets the most recently added item directly for testing purposes.
     */
    fun setMostRecentlyAdded(item: CollectionItem?) {
        _mostRecentlyAdded.value = item
    }

    /**
     * Sets the first unplayed game directly for testing purposes.
     */
    fun setFirstUnplayed(item: CollectionItem?) {
        _firstUnplayed.value = item
    }

    private fun updateComputedValues(items: List<CollectionItem>) {
        _collectionCount.value = items.size.toLong()
        // Note: unplayed count should be set manually in tests as it depends on plays data
        _mostRecentlyAdded.value = items
            .filter { it.lastModifiedDate != null }
            .maxByOrNull { it.lastModifiedDate!! }
        // Note: first unplayed should be set manually in tests as it depends on plays data
    }
}
