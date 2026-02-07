package app.meeplebook.core.collection.model

/**
 * Represents a simplified game entry for autocomplete/selection.
 *
 * @param id The BGG game ID.
 * @param name The name of the game.
 * @param thumbnailUrl The URL to the game's thumbnail image.
 * @param yearPublished The year the game was published.
 */
data class GameSummary(
    val id: Long,
    val name: String,
    val thumbnailUrl: String?,
    val yearPublished: Int?
)
