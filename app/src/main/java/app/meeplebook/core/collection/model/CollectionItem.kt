package app.meeplebook.core.collection.model

/**
 * Represents a board game in the user's BGG collection.
 */
data class CollectionItem(
    val objectId: Long,
    val name: String,
    val yearPublished: Int?,
    val thumbnailUrl: String?,
    val imageUrl: String?,
    val numPlays: Int,
    val owned: Boolean,
    val rating: Float?,
    val averageRating: Float?
)
