package app.meeplebook.core.database.converters

import androidx.room.TypeConverter
import app.meeplebook.core.sync.model.SyncType

class SyncTypeConverters {

    @TypeConverter
    fun fromSyncType(value: SyncType): String {
        return value.name
    }

    @TypeConverter
    fun toSyncType(value: String): SyncType {
        return SyncType.valueOf(value)
    }
}