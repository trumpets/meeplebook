package app.meeplebook.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation
import app.meeplebook.core.plays.model.Play

/**
 * Room relation representing a play with its players.
 */
data class PlayWithPlayers(
    @Embedded val play: PlayEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playId"
    )
    val players: List<PlayerEntity>
)

/**
 * Maps a [PlayWithPlayers] to a [Play] domain model.
 */
fun PlayWithPlayers.toPlay(): Play {
    return play.toPlay(players.map { it.toPlayer() })
}
