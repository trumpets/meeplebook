package app.meeplebook.core.collection

import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.remote.BggCollectionRemoteDataSource
import app.meeplebook.core.collection.remote.CollectionNotReadyException
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import javax.inject.Inject

/**
 * Implementation of [CollectionRepository] that fetches from remote
 * and caches in memory.
 */
class CollectionRepositoryImpl @Inject constructor(
    private val remote: BggCollectionRemoteDataSource
) : CollectionRepository {

    private val _cachedCollection = MutableStateFlow<List<CollectionItem>>(emptyList())

    override suspend fun getCollection(username: String): AppResult<List<CollectionItem>, CollectionError> {
        return try {
            val items = remote.getCollection(username)
            _cachedCollection.value = items
            AppResult.Success(items)
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException -> AppResult.Failure(CollectionError.NotLoggedIn)
                is IOException -> AppResult.Failure(CollectionError.NetworkError)
                is CollectionNotReadyException -> AppResult.Failure(CollectionError.Timeout)
                else -> AppResult.Failure(CollectionError.Unknown(e))
            }
        }
    }

    override fun observeCollection(): Flow<List<CollectionItem>> {
        return _cachedCollection.asStateFlow()
    }
}
