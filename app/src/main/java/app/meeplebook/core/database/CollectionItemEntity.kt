package app.meeplebook.core.database

import androidx.room.Entity
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype

/**
 * Room entity representing a collection item stored locally.
 */
@Entity(
    tableName = "collection_items",
    primaryKeys = ["gameId"]
)
data class CollectionItemEntity(
    val gameId: Int,
    val subtype: GameSubtype,
    val name: String,
    val yearPublished: Int?,
    val thumbnail: String?
)

/**
 * Maps a [CollectionItemEntity] to a [CollectionItem] domain model.
 */
fun CollectionItemEntity.toCollectionItem(): CollectionItem {
    return CollectionItem(
        gameId = gameId,
        subtype = subtype,
        name = name,
        yearPublished = yearPublished,
        thumbnail = thumbnail
    )
}

/**
 * Maps a [CollectionItem] to a [CollectionItemEntity] for storage.
 */
fun CollectionItem.toEntity(): CollectionItemEntity {
    return CollectionItemEntity(
        gameId = gameId,
        subtype = subtype,
        name = name,
        yearPublished = yearPublished,
        thumbnail = thumbnail
    )
}
