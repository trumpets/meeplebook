package app.meeplebook.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import java.time.Instant

/**
 * Room entity representing a collection item stored locally.
 */
@Entity(
    tableName = "collection_items"
)
data class CollectionItemEntity(
    @PrimaryKey
    val gameId: Int,
    val subtype: GameSubtype,
    val name: String,
    val yearPublished: Int?,
    val thumbnail: String?,
    val lastModified: Instant?
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
        thumbnail = thumbnail,
        lastModified = lastModified
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
        thumbnail = thumbnail,
        lastModified = lastModified
    )
}
