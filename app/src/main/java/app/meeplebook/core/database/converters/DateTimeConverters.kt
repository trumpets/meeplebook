package app.meeplebook.core.database.converters

import androidx.room.TypeConverter
import java.time.Instant

class DateTimeConverters {

    @TypeConverter
    fun fromInstant(value: Instant?): Long? {
        return value?.toEpochMilli()
    }

    @TypeConverter
    fun toInstant(value: Long?): Instant? {
        return value?.let {
            Instant.ofEpochMilli(it)
        }
    }
}