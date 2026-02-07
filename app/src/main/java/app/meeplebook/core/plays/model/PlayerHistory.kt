package app.meeplebook.core.plays.model

/**
 * Represents a player's history at a specific location.
 * Used for quick-add suggestions when logging plays at a known location.
 *
 * @param name The display name of the player.
 * @param username The BGG username of the player (if linked).
 * @param userId The BGG user ID (if linked).
 * @param playCount The number of times this player has played at this location.
 */
data class PlayerHistory(
    val name: String,
    val username: String? = null,
    val userId: Long? = null,
    val playCount: Int
)
