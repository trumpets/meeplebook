package app.meeplebook.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.meeplebook.core.plays.model.Player

/**
 * Room entity representing a player in a play.
 */
@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = PlayEntity::class,
            parentColumns = ["id"],
            childColumns = ["playId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playId")]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val playId: Int,
    val username: String?,
    val userId: Int?,
    val name: String,
    val startPosition: String?,
    val color: String?,
    val score: String?,
    val win: Boolean
)

/**
 * Maps a [PlayerEntity] to a [Player] domain model.
 */
fun PlayerEntity.toPlayer(): Player {
    return Player(
        id = id,
        playId = playId,
        username = username,
        userId = userId,
        name = name,
        startPosition = startPosition,
        color = color,
        score = score,
        win = win
    )
}

/**
 * Maps a [Player] to a [PlayerEntity] for storage.
 */
fun Player.toEntity(): PlayerEntity {
    return PlayerEntity(
        id = id,
        playId = playId,
        username = username,
        userId = userId,
        name = name,
        startPosition = startPosition,
        color = color,
        score = score,
        win = win
    )
}
