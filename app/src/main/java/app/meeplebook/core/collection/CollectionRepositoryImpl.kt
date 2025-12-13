package app.meeplebook.core.collection

import app.meeplebook.core.collection.local.CollectionLocalDataSource
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.remote.CollectionFetchException
import app.meeplebook.core.collection.remote.CollectionRemoteDataSource
import app.meeplebook.core.network.RetryException
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

    override fun observeCollection(): Flow<List<CollectionItem>> {
        return local.observeCollection()
    }

    override suspend fun getCollection(): List<CollectionItem> {
        return local.getCollection()
    }

    override suspend fun syncCollection(username: String): AppResult<List<CollectionItem>, CollectionError> {
        try {
            val items = remote.fetchCollection(username)
            local.saveCollection(items)
            return AppResult.Success(items)
        } catch (e: Exception) {
            return when (e) {
                is IllegalArgumentException -> AppResult.Failure(CollectionError.NotLoggedIn)
                is IOException -> AppResult.Failure(CollectionError.NetworkError)
                is RetryException -> AppResult.Failure(CollectionError.MaxRetriesExceeded(e))
                is CollectionFetchException -> AppResult.Failure(CollectionError.Unknown(e))
                else -> AppResult.Failure(CollectionError.Unknown(e))
            }
        }
    }

    override suspend fun clearCollection() {
        local.clearCollection()
    }

    override suspend fun getCollectionCount(): Int {
        return local.getCollectionCount()
    }

    override suspend fun getUnplayedGamesCount(): Int {
        return local.getUnplayedGamesCount()
    }

    override suspend fun getMostRecentlyAddedItem(): CollectionItem? {
        return local.getMostRecentlyAddedItem()
    }

    override suspend fun getFirstUnplayedGame(): CollectionItem? {
        return local.getFirstUnplayedGame()
    }
}
