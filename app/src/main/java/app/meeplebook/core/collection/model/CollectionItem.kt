package app.meeplebook.core.collection.model

import java.time.Instant

/**
 * Represents a game item in a user's BGG collection.
 *
 * @param gameId The BGG game ID.
 * @param subtype The type of the game (boardgame or boardgameexpansion).
 * @param name The name of the game.
 * @param yearPublished The year the game was published.
 * @param thumbnail URL to the game's thumbnail image.
 */
data class CollectionItem(
    val gameId: Int,
    val subtype: GameSubtype,
    val name: String,
    val yearPublished: Int?,
    val thumbnail: String?,
    val lastModifiedDate: Instant?
)

/**
 * The subtype of a board game item.
 */
enum class GameSubtype {
    BOARDGAME,
    BOARDGAME_EXPANSION
}
