package app.meeplebook.core.plays.remote.dto

import java.time.Instant

/**
 * Data Transfer Object representing a play retrieved from the remote service (BGG).
 *
 * This DTO mirrors the remote API shape and is used by network parsing and
 * synchronization code. It is mapped to local entities for persistence.
 *
 * @property remoteId The unique identifier assigned by the remote service for this play.
 * @property date The instant when the play occurred on the remote service.
 * @property quantity Number of times the game was played in this record (usually 1).
 * @property length Length of the play in minutes, or null if not provided.
 * @property incomplete True if the play was recorded as incomplete.
 * @property location Optional location string reported by the remote service.
 * @property gameId Remote identifier of the played game.
 * @property gameName Display name of the game at the time of the play.
 * @property comments Optional free-text comments attached to the play on the remote service.
 * @property players List of players participating in the play as provided by the remote service.
 */
data class RemotePlayDto(
    val remoteId: Long,
    val date: Instant,
    val quantity: Int,
    val length: Int?,
    val incomplete: Boolean,
    val location: String?,
    val gameId: Long,
    val gameName: String,
    val comments: String?,
    val players: List<RemotePlayerDto>
)