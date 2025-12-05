package app.meeplebook.core.database

import androidx.room.Entity
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype

/**
 * Room entity representing a collection item stored locally.
 */
@Entity(
    tableName = "collection_items",
    primaryKeys = ["gameId", "username"]
)
data class CollectionItemEntity(
    val gameId: Int,
    val subtype: String,
    val name: String,
    val yearPublished: Int?,
    val thumbnail: String?,
    val username: String
)

/**
 * Maps a [CollectionItemEntity] to a [CollectionItem] domain model.
 */
fun CollectionItemEntity.toCollectionItem(): CollectionItem {
    return CollectionItem(
        gameId = gameId,
        subtype = when (subtype) {
            "boardgameexpansion" -> GameSubtype.BOARDGAME_EXPANSION
            else -> GameSubtype.BOARDGAME
        },
        name = name,
        yearPublished = yearPublished,
        thumbnail = thumbnail
    )
}

/**
 * Maps a [CollectionItem] to a [CollectionItemEntity] for storage.
 */
fun CollectionItem.toEntity(username: String): CollectionItemEntity {
    return CollectionItemEntity(
        gameId = gameId,
        subtype = when (subtype) {
            GameSubtype.BOARDGAME -> "boardgame"
            GameSubtype.BOARDGAME_EXPANSION -> "boardgameexpansion"
        },
        name = name,
        yearPublished = yearPublished,
        thumbnail = thumbnail,
        username = username
    )
}
