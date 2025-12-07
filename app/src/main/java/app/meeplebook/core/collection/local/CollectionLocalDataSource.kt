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
     * @return Flow emitting the user's collection.
     */
    fun observeCollection(): Flow<List<CollectionItem>>

    /**
     * Gets the collection for a specific user.
     *
     * @return The user's collection.
     */
    suspend fun getCollection(): List<CollectionItem>

    /**
     * Saves (replaces) the collection for a specific user.
     *
     * @param items The collection items to save.
     */
    suspend fun saveCollection(items: List<CollectionItem>)

    /**
     * Clears the collection for a specific user.
     *
     */
    suspend fun clearCollection()
}
