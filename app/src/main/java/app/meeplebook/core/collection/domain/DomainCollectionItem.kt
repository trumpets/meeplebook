package app.meeplebook.core.collection.domain

import app.meeplebook.core.collection.model.CollectionItem

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