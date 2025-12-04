package app.meeplebook.core.collection

import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user's board game collection.
 */
interface CollectionRepository {

    /**
     * Fetches the collection for a user.
     *
     * @param username BGG username
     * @return Result containing list of collection items or error
     */
    suspend fun getCollection(username: String): AppResult<List<CollectionItem>, CollectionError>

    /**
     * Observes the cached collection items.
     * Emits empty list when no cached data is available.
     */
    fun observeCollection(): Flow<List<CollectionItem>>
}
