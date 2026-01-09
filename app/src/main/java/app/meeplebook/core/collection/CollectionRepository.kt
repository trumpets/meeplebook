package app.meeplebook.core.collection

import app.meeplebook.core.collection.model.CollectionDataQuery
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing user collections.
 */
interface CollectionRepository {

    /**
     * Observes the collection from local storage.
     *
     * @return Flow emitting the user's collection.
     */
    fun observeCollection(query: CollectionDataQuery? = null): Flow<List<CollectionItem>>

    /**
     * Gets the collection from local storage.
     *
     * @return The user's collection.
     */
    suspend fun getCollection(): List<CollectionItem>

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
     * Clears the collection from local storage.
     *
     */
    suspend fun clearCollection()

    /**
     * Observe the count of items in the collection.
     */
    fun observeCollectionCount(): Flow<Long>

    /**
     * Observe the count of unplayed games in the collection.
     */
    fun observeUnplayedGamesCount(): Flow<Long>

    /**
     * Observe the most recently added collection item.
     */
    fun observeMostRecentlyAddedItem(): Flow<CollectionItem?>

    /**
     * Observe the first unplayed game from the collection.
     */
    fun observeFirstUnplayedGame(): Flow<CollectionItem?>
}
