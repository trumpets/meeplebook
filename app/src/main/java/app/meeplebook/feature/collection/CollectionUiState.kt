package app.meeplebook.feature.collection

import app.meeplebook.core.collection.model.CollectionItem

/**
 * Represents a game item in the collection with display metadata.
 */
data class CollectionGameItem(
    val gameId: Int,
    val name: String,
    val yearPublished: Int?,
    val thumbnail: String?,
    val playCount: Int = 0,
    val lastPlayed: String? = null
)

/**
 * Sort options for the collection.
 */
enum class CollectionSort {
    ALPHABETICAL,
    YEAR_PUBLISHED_OLDEST,
    YEAR_PUBLISHED_NEWEST,
    MOST_PLAYED,
    LEAST_PLAYED,
    MOST_RECENTLY_PLAYED,
    LEAST_RECENTLY_PLAYED
}

/**
 * UI state for the Collection screen.
 */
data class CollectionUiState(
    val games: List<CollectionGameItem> = emptyList(),
    val currentSort: CollectionSort = CollectionSort.ALPHABETICAL,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Extension function to convert [CollectionItem] to [CollectionGameItem].
 * 
 * Note: playCount and lastPlayed are set to default values here.
 * When integrated with a ViewModel, these should be populated by merging
 * collection data with plays data from PlaysRepository.
 */
fun CollectionItem.toCollectionGameItem(): CollectionGameItem {
    return CollectionGameItem(
        gameId = gameId,
        name = name,
        yearPublished = yearPublished,
        thumbnail = thumbnail,
        playCount = 0, // Will be populated from plays data in ViewModel
        lastPlayed = null // Will be populated from plays data in ViewModel
    )
}
