package app.meeplebook.core.plays.model

/**
 * Represents a player in a play.
 *
 * @param id The unique ID of this player record (auto-generated, 0 for new players).
 * @param playId The play ID this player is associated with.
 * @param username The BGG username of the player.
 * @param userId The BGG user ID.
 * @param name The display name of the player.
 * @param startPosition The starting position of the player.
 * @param color The color assigned to the player.
 * @param score The score achieved by the player.
 * @param win Whether the player won.
 */
data class Player(
    val id: Long = 0,
    val playId: Long,
    val username: String?,
    val userId: Long?,
    val name: String,
    val startPosition: String?,
    val color: String?,
    val score: String?,
    val win: Boolean
)