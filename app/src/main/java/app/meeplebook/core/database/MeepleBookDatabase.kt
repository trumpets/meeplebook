package app.meeplebook.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The Room database for MeepleBook.
 */
@Database(
    entities = [CollectionItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MeepleBookDatabase : RoomDatabase() {
    abstract fun collectionItemDao(): CollectionItemDao
}
