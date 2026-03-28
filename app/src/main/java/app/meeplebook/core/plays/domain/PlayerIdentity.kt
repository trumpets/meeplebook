package app.meeplebook.core.plays.domain

/**
 * Represents the identity of a player in a play, including their name
 * and optional BGG username and user ID.
 */
data class PlayerIdentity(
    val name: String,
    val username: String?,
    val userId: Long?
)