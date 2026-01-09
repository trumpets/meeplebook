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
 * @param lastModifiedDate The last modified date of the collection item.
 * @param minPlayers Minimum number of players.
 * @param maxPlayers Maximum number of players.
 * @param minPlayTimeMinutes Minimum play time in minutes.
 * @param maxPlayTimeMinutes Maximum play time in minutes.
 * @param numPlays The number of times the game has been played.
 */
data class CollectionItem(
    val gameId: Long,
    val subtype: GameSubtype,
    val name: String,
    val yearPublished: Int?,
    val thumbnail: String?,
    val lastModifiedDate: Instant?,
    val minPlayers: Int?,
    val maxPlayers: Int?,
    val minPlayTimeMinutes: Int?,
    val maxPlayTimeMinutes: Int?,
    val numPlays: Int
)

/**
 * The subtype of a board game item.
 */
enum class GameSubtype {
    BOARDGAME,
    BOARDGAME_EXPANSION
}
