package app.meeplebook.core.collection

import app.meeplebook.core.collection.local.CollectionLocalDataSource
import app.meeplebook.core.collection.model.CollectionDataQuery
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.QuickFilter
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

    override fun observeCollection(query: CollectionDataQuery?): Flow<List<CollectionItem>> {
        return when (query?.quickFilter) {
            QuickFilter.UNPLAYED -> local.observeCollectionUnplayed()
            else -> {
                val searchQuery = query?.searchQuery.orEmpty()
                if (searchQuery.isBlank()) {
                    local.observeCollection()
                } else {
                    local.observeCollectionByName(searchQuery)
                }
            }
        }
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

    override fun observeCollectionCount(): Flow<Long> {
        return local.observeCollectionCount()
    }

    override fun observeUnplayedGamesCount(): Flow<Long> {
        return local.observeUnplayedGamesCount()
    }

    override fun observeMostRecentlyAddedItem(): Flow<CollectionItem?> {
        return local.observeMostRecentlyAddedItem()
    }

    override fun observeFirstUnplayedGame(): Flow<CollectionItem?> {
        return local.observeFirstUnplayedGame()
    }
}
