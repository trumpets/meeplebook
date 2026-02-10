package app.meeplebook.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.meeplebook.core.plays.domain.CreatePlayCommand
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.plays.model.Player
import app.meeplebook.core.plays.remote.dto.RemotePlayDto
import java.time.Instant

/**
 * Room entity representing a play stored locally.
 */
@Entity(
    tableName = "plays",
    indices = [
        Index(value = ["date"]),
        Index(value = ["gameId"]),
        Index(value = ["gameId", "date"]),
        Index(
            value = ["remoteId"],
            unique = true
        )
    ]
)
data class PlayEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0L,
    val remoteId: Long?,
    val date: Instant,
    val quantity: Int,
    val length: Int?,
    val incomplete: Boolean,
    val location: String?,
    val gameId: Long,
    val gameName: String,
    val comments: String?,
    val syncStatus: PlaySyncStatus
)

/**
 * Maps a [PlayEntity] to a [Play] domain model.
 * Note: Players must be loaded separately and combined.
 */
fun PlayEntity.toPlay(players: List<Player>): Play {
    return Play(
        playId = when (remoteId) {
            null -> PlayId.Local(localId)
            else -> PlayId.Remote(localId, remoteId)
        },
        date = date,
        quantity = quantity,
        length = length,
        incomplete = incomplete,
        location = location,
        gameId = gameId,
        gameName = gameName,
        comments = comments,
        players = players,
        syncStatus = syncStatus
    )
}

/**
 * Maps a [RemotePlayDto] to a [PlayEntity] for storage.
 */
fun RemotePlayDto.toEntity(localId: Long, syncStatus: PlaySyncStatus): PlayEntity {
    return PlayEntity(
        localId = localId,
        remoteId = remoteId,
        date = date,
        quantity = quantity,
        length = length,
        incomplete = incomplete,
        location = location,
        gameId = gameId,
        gameName = gameName,
        comments = comments,
        syncStatus = syncStatus
    )
}

/**
 * Maps a [CreatePlayCommand] to a [PlayEntity] for storage.
 */
fun CreatePlayCommand.toEntity(): PlayEntity {
    return PlayEntity(
        localId = 0, // Will be auto-generated
        remoteId = null, // New play, no remote ID yet
        date = date,
        quantity = quantity,
        length = length,
        incomplete = incomplete,
        location = location,
        gameId = gameId,
        gameName = gameName,
        comments = comments,
        syncStatus = PlaySyncStatus.PENDING
    )
}