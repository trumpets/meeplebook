package app.meeplebook.core.collection.local

import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSummary
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
     * Observes the collection filtered by name.
     *
     * @return Flow emitting the user's collection filtered by name.
     */
    fun observeCollectionByName(nameQuery: String): Flow<List<CollectionItem>>

    /**
     * Observes the unplayed items in the collection.
     *
     * @return Flow emitting the user's unplayed collection items.
     */
    fun observeCollectionUnplayed(): Flow<List<CollectionItem>>

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
    fun observeMostRecentlyAddedItem(): Flow<CollectionItem?>

    /**
     * Observe the first unplayed game.
     */
    fun observeFirstUnplayedGame(): Flow<CollectionItem?>

    /**
     * Search collection for game names matching query (for autocomplete).
     */
    suspend fun searchGamesForAutocomplete(query: String, limit: Int = 20): List<GameSummary>
}
