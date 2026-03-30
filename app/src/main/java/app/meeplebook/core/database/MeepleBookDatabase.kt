package app.meeplebook.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.meeplebook.core.database.converters.DateTimeConverters
import app.meeplebook.core.database.converters.GameRankConverter
import app.meeplebook.core.database.dao.CollectionItemDao
import app.meeplebook.core.database.dao.PlayDao
import app.meeplebook.core.database.dao.PlayerDao
import app.meeplebook.core.database.entity.CollectionItemEntity
import app.meeplebook.core.database.entity.PlayEntity
import app.meeplebook.core.database.entity.PlayerEntity

/**
 * The Room database for MeepleBook.
 *
 * Version history:
 * - 1: Initial schema (collection_items, plays, players)
 * - 2: Added image, userRating, ranks columns to collection_items.
 *      Handled by [fallbackToDestructiveMigration] until explicit migrations are added before release.
 */
@Database(
    entities = [
        CollectionItemEntity::class,
        PlayEntity::class,
        PlayerEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateTimeConverters::class, GameRankConverter::class)
abstract class MeepleBookDatabase : RoomDatabase() {
    abstract fun collectionItemDao(): CollectionItemDao
    abstract fun playDao(): PlayDao
    abstract fun playerDao(): PlayerDao
}
