package app.meeplebook.core.collection

import app.meeplebook.core.collection.local.CollectionLocalDataSource
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.remote.CollectionFetchException
import app.meeplebook.core.collection.remote.CollectionRemoteDataSource
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import javax.inject.Inject

/**
 * Implementation of [CollectionRepository].
 */
class CollectionRepositoryImpl @Inject constructor(
    private val local: CollectionLocalDataSource,
    private val remote: CollectionRemoteDataSource
) : CollectionRepository {

    override fun observeCollection(username: String): Flow<List<CollectionItem>> {
        return local.observeCollection(username)
    }

    override suspend fun getCollection(username: String): List<CollectionItem> {
        return local.getCollection(username)
    }

    override suspend fun syncCollection(username: String): AppResult<List<CollectionItem>, CollectionError> {
        return try {
            val items = remote.fetchCollection(username)
            local.saveCollection(username, items)
            AppResult.Success(items)
        } catch (e: Exception) {
            AppResult.Failure(mapException(e))
        }
    }

    override suspend fun clearCollection(username: String) {
        local.clearCollection(username)
    }

    private fun mapException(e: Exception): CollectionError {
        return when (e) {
            is IllegalArgumentException -> CollectionError.NotLoggedIn
            is IOException -> CollectionError.NetworkError
            is CollectionFetchException -> {
                when {
                    e.message?.contains("Max retry") == true -> CollectionError.MaxRetriesExceeded
                    e.message?.contains("Server error") == true -> CollectionError.RateLimitError
                    else -> CollectionError.Unknown(e)
                }
            }
            else -> CollectionError.Unknown(e)
        }
    }
}
