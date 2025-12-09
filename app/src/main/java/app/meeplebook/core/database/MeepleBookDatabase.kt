package app.meeplebook.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import app.meeplebook.core.database.dao.CollectionItemDao
import app.meeplebook.core.database.dao.PlayDao
import app.meeplebook.core.database.dao.PlayerDao
import app.meeplebook.core.database.entity.CollectionItemEntity
import app.meeplebook.core.database.entity.PlayEntity
import app.meeplebook.core.database.entity.PlayerEntity

/**
 * The Room database for MeepleBook.
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
abstract class MeepleBookDatabase : RoomDatabase() {
    abstract fun collectionItemDao(): CollectionItemDao
    abstract fun playDao(): PlayDao
    abstract fun playerDao(): PlayerDao
}
