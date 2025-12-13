package app.meeplebook.core.collection.local

import app.meeplebook.core.collection.model.CollectionItem
import kotlinx.coroutines.flow.Flow

/**
 * Local data source for storing and retrieving user collections.
 */
interface CollectionLocalDataSource {

    /**
     * Observes the collection.
     *
     * @return Flow emitting the user's collection.
     */
    fun observeCollection(): Flow<List<CollectionItem>>

    /**
     * Gets the collection.
     *
     * @return The user's collection.
     */
    suspend fun getCollection(): List<CollectionItem>

    /**
     * Saves (replaces) the collection.
     *
     * @param items The collection items to save.
     */
    suspend fun saveCollection(items: List<CollectionItem>)

    /**
     * Clears the collection.
     *
     */
    suspend fun clearCollection()

    /**
     * Gets the count of items in the collection.
     */
    suspend fun getCollectionCount(): Long

    /**
     * Gets the count of unplayed games.
     */
    suspend fun getUnplayedGamesCount(): Long

    /**
     * Gets the most recently added collection item.
     */
    suspend fun getMostRecentlyAddedItem(): CollectionItem?

    /**
     * Gets the first unplayed game.
     */
    suspend fun getFirstUnplayedGame(): CollectionItem?
}
