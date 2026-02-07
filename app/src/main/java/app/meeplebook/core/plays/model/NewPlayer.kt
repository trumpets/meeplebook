package app.meeplebook.core.plays.model

/**
 * Represents a player being added to a new play.
 *
 * @param name The display name of the player.
 * @param username The BGG username of the player (if linked to BGG account).
 * @param userId The BGG user ID (if linked to BGG account).
 * @param startPosition The starting position of the player.
 * @param color The color assigned to the player.
 * @param score The score achieved by the player.
 * @param win Whether the player won.
 * @param team The team the player is on (for team-based games).
 */
data class NewPlayer(
    val name: String,
    val username: String? = null,
    val userId: Long? = null,
    val startPosition: String? = null,
    val color: String? = null,
    val score: Int? = null,
    val win: Boolean = false,
    val team: String? = null
)
