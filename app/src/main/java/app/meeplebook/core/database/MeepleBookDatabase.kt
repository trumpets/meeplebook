package app.meeplebook.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

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
