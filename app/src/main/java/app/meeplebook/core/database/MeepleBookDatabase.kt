package app.meeplebook.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.meeplebook.core.database.converters.DateTimeConverters
import app.meeplebook.core.database.converters.SyncTypeConverters
import app.meeplebook.core.database.dao.CollectionItemDao
import app.meeplebook.core.database.dao.PlayDao
import app.meeplebook.core.database.dao.PlayerDao
import app.meeplebook.core.database.dao.SyncDao
import app.meeplebook.core.database.entity.CollectionItemEntity
import app.meeplebook.core.database.entity.PlayEntity
import app.meeplebook.core.database.entity.PlayerEntity
import app.meeplebook.core.database.entity.SyncStateEntity

/**
 * The Room database for MeepleBook.
 */
@Database(
    entities = [
        CollectionItemEntity::class,
        PlayEntity::class,
        PlayerEntity::class,
        SyncStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateTimeConverters::class, SyncTypeConverters::class)
abstract class MeepleBookDatabase : RoomDatabase() {
    abstract fun collectionItemDao(): CollectionItemDao
    abstract fun playDao(): PlayDao
    abstract fun playerDao(): PlayerDao
    abstract fun syncDao(): SyncDao
}
