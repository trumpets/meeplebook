package app.meeplebook.core.plays.model

/**
 * Represents a player in a play.
 *
 * @property id The unique ID of this player record (auto-generated, 0 for new players).
 * @property playId The play ID this player is associated with.
 * @property username The BGG username of the player.
 * @property userId The BGG user ID.
 * @property name The display name of the player.
 * @property startPosition The starting position of the player.
 * @property color The color assigned to the player.
 * @property score The score achieved by the player.
 * @property win Whether the player won.
 */
data class Player(
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