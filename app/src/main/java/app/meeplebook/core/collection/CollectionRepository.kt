package app.meeplebook.core.collection

import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing user collections.
 */
interface CollectionRepository {

    /**
     * Observes the collection for a specific user from local storage.
     *
     * @param username The BGG username.
     * @return Flow emitting the user's collection.
     */
    fun observeCollection(username: String): Flow<List<CollectionItem>>

    /**
     * Gets the collection for a specific user from local storage.
     *
     * @param username The BGG username.
     * @return The user's collection.
     */
    suspend fun getCollection(username: String): List<CollectionItem>

    /**
     * Syncs the collection for a specific user from BGG.
     *
     * Fetches the collection from BGG and stores it locally.
     *
     * @param username The BGG username.
     * @return Success with the collection, or Failure with an error.
     */
    suspend fun syncCollection(username: String): AppResult<List<CollectionItem>, CollectionError>

    /**
     * Clears the collection for a specific user from local storage.
     *
     * @param username The BGG username.
     */
    suspend fun clearCollection(username: String)
}
