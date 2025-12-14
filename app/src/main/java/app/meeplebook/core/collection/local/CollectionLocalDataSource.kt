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
     * Observe the count of items in the collection.
     */
    fun observeCollectionCount(): Flow<Long>

    /**
     * Observe the count of unplayed games.
     */
    fun observeUnplayedGamesCount(): Flow<Long>

    /**
     * Observe the most recently added collection item.
     */
    fun observeMostRecentlyAddedItem(): Flow<CollectionItem>

    /**
     * Observe the first unplayed game.
     */
    fun observeFirstUnplayedGame(): Flow<CollectionItem>
}
