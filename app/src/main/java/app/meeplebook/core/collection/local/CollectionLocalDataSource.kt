package app.meeplebook.core.collection.local

import app.meeplebook.core.collection.model.CollectionItem
import kotlinx.coroutines.flow.Flow

/**
 * Local data source for storing and retrieving user collections.
 */
interface CollectionLocalDataSource {

    /**
     * Observes the collection for a specific user.
     *
     * @param username The BGG username.
     * @return Flow emitting the user's collection.
     */
    fun observeCollection(username: String): Flow<List<CollectionItem>>

    /**
     * Gets the collection for a specific user.
     *
     * @param username The BGG username.
     * @return The user's collection.
     */
    suspend fun getCollection(username: String): List<CollectionItem>

    /**
     * Saves (replaces) the collection for a specific user.
     *
     * @param username The BGG username.
     * @param items The collection items to save.
     */
    suspend fun saveCollection(username: String, items: List<CollectionItem>)

    /**
     * Clears the collection for a specific user.
     *
     * @param username The BGG username.
     */
    suspend fun clearCollection(username: String)
}
