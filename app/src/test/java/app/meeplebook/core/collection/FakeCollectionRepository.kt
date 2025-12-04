package app.meeplebook.core.collection

import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [CollectionRepository] for testing.
 */
class FakeCollectionRepository : CollectionRepository {

    private val _collection = MutableStateFlow<List<CollectionItem>>(emptyList())

    var getCollectionResult: AppResult<List<CollectionItem>, CollectionError> =
        AppResult.Failure(CollectionError.Unknown(IllegalStateException("Not configured")))

    var getCollectionCallCount = 0
        private set
    var lastUsername: String? = null
        private set

    override suspend fun getCollection(username: String): AppResult<List<CollectionItem>, CollectionError> {
        getCollectionCallCount++
        lastUsername = username

        when (val result = getCollectionResult) {
            is AppResult.Success -> _collection.value = result.data
            is AppResult.Failure -> { /* no-op */ }
        }

        return getCollectionResult
    }

    override fun observeCollection(): Flow<List<CollectionItem>> = _collection

    fun setCollection(items: List<CollectionItem>) {
        _collection.value = items
    }
}
