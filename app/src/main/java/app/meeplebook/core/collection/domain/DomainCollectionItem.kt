package app.meeplebook.core.collection.domain

import app.meeplebook.core.collection.model.CollectionItem

/**
 * Domain model representing a collection item for UI display purposes.
 *
 * This is a simplified version of [CollectionItem] that contains only the data
 * needed for collection list display. Unlike [CollectionItem], this model:
 * - Excludes metadata fields like lastModifiedDate and subtype
 * - Uses raw numeric counts (playCount, player counts, play times)
 * - Renames thumbnail field to thumbnailUrl for clarity
 *
 * @property gameId The BGG game ID.
 * @property name The name of the game.
 * @property yearPublished The year the game was published.
 * @property thumbnailUrl URL to the game's thumbnail image.
 * @property playCount The number of times the game has been played.
 * @property minPlayers Minimum number of players.
 * @property maxPlayers Maximum number of players.
 * @property minPlayTimeMinutes Minimum play time in minutes.
 * @property maxPlayTimeMinutes Maximum play time in minutes.
 */
data class DomainCollectionItem(
    val gameId: Long,
    val name: String,
    val yearPublished: Int?,
    val thumbnailUrl: String?,

    // raw counts (NOT strings)
    val playCount: Int,
    val minPlayers: Int?,
    val maxPlayers: Int?,
    val minPlayTimeMinutes: Int?,
    val maxPlayTimeMinutes: Int?
)

/**
 * Maps a [CollectionItem] to a [DomainCollectionItem] for collection display.
 */
fun CollectionItem.toDomainCollectionItem(): DomainCollectionItem {
    return DomainCollectionItem(
        gameId = gameId,
        name = name,
        yearPublished = yearPublished,
        thumbnailUrl = thumbnail,
        playCount = numPlays,
        minPlayers = minPlayers,
        maxPlayers = maxPlayers,
        minPlayTimeMinutes = minPlayTimeMinutes,
        maxPlayTimeMinutes = maxPlayTimeMinutes
    )
}
