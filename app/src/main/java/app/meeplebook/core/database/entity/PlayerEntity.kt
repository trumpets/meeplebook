package app.meeplebook.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.meeplebook.core.plays.domain.CreatePlayerCommand
import app.meeplebook.core.plays.model.Player
import app.meeplebook.core.plays.remote.dto.RemotePlayerDto

/**
 * Room entity representing a player in a play.
 */
@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = PlayEntity::class,
            parentColumns = ["localId"],
            childColumns = ["playId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playId"]),
        Index(value = ["name", "username"])
    ]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playId: Long,
    val username: String?,
    val userId: Long?,
    val name: String,
    val startPosition: String?,
    val color: String?,
    val score: Int?,
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
 * Maps a [RemotePlayerDto] to a [PlayerEntity] for storage.
 * The [playId] must be provided to associate the player with the correct play.
 */
fun RemotePlayerDto.toEntity(playId: Long): PlayerEntity {
    return PlayerEntity(
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
 * Maps a [CreatePlayerCommand] to a [PlayerEntity] for storage.
 */
fun CreatePlayerCommand.toEntity(): PlayerEntity {
    return PlayerEntity(
        playId = 0, // Placeholder, will be set before persistence
        username = username,
        userId = userId,
        name = name,
        startPosition = startPosition,
        color = color,
        score = score,
        win = win
    )
}