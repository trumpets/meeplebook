package app.meeplebook.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.Player

/**
 * Room entity representing a play stored locally.
 */
@Entity(
    tableName = "plays",
    indices = [Index(value = ["date"])]
)
data class PlayEntity(
    @PrimaryKey
    val id: Int,
    val date: String,
    val quantity: Int,
    val length: Int?,
    val incomplete: Boolean,
    val location: String?,
    val gameId: Int,
    val gameName: String,
    val comments: String?
)

/**
 * Maps a [PlayEntity] to a [Play] domain model.
 * Note: Players must be loaded separately and combined.
 */
fun PlayEntity.toPlay(players: List<Player>): Play {
    return Play(
        id = id,
        date = date,
        quantity = quantity,
        length = length,
        incomplete = incomplete,
        location = location,
        gameId = gameId,
        gameName = gameName,
        comments = comments,
        players = players
    )
}

/**
 * Maps a [Play] to a [PlayEntity] for storage.
 */
fun Play.toEntity(): PlayEntity {
    return PlayEntity(
        id = id,
        date = date,
        quantity = quantity,
        length = length,
        incomplete = incomplete,
        location = location,
        gameId = gameId,
        gameName = gameName,
        comments = comments
    )
}
