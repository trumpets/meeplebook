package app.meeplebook.core.collection.model

import java.time.Instant

/**
 * Represents a game item in a user's BGG collection.
 *
 * @property gameId The BGG game ID.
 * @property subtype The type of the game (boardgame or boardgameexpansion).
 * @property name The name of the game.
 * @property yearPublished The year the game was published.
 * @property thumbnail URL to the game's thumbnail image.
 * @property lastModifiedDate The last modified date of the collection item.
 * @property minPlayers Minimum number of players.
 * @property maxPlayers Maximum number of players.
 * @property minPlayTimeMinutes Minimum play time in minutes.
 * @property maxPlayTimeMinutes Maximum play time in minutes.
 * @property numPlays The number of times the game has been played.
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
