package app.meeplebook.core.plays.domain

import java.time.Instant

/**
 * Command describing the data required to create a new play record.
 *
 * This is a simple DTO used by use-cases and repositories when a user
 * creates or submits a play. It contains all play-level information and
 * the list of players that participated.
 *
 * @property date The instant when the play occurred (user local time should be converted to Instant by caller).
 * @property quantity Number of times the game was played in this record (usually 1).
 * @property length Length of the play in minutes, or null if unknown.
 * @property incomplete True when the play was not completed (e.g., game interrupted).
 * @property location Optional textual location where the play took place (venue, etc.).
 * @property gameId The identifier of the game that was played.
 * @property gameName Display name of the game at the time of play (stored for denormalization and history).
 * @property comments Optional free-text comments attached to the play.
 * @property players The list of players who participated in the play. Use [CreatePlayerCommand] to describe each player.
 */
data class CreatePlayCommand(
    val date: Instant,
    val quantity: Int,
    val length: Int?,
    val incomplete: Boolean,
    val location: String?,
    val gameId: Long,
    val gameName: String,
    val comments: String?,
    val players: List<CreatePlayerCommand>
)