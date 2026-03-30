package app.meeplebook.core.database.converters

import androidx.room.TypeConverter
import app.meeplebook.core.collection.model.GameRank
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room [TypeConverter]s for [List]<[GameRank]> ↔ JSON [String].
 */
class GameRankConverter {

    @TypeConverter
    fun fromList(ranks: List<GameRank>): String = Json.encodeToString(ranks)

    @TypeConverter
    fun toList(value: String): List<GameRank> = Json.decodeFromString(value)
}
