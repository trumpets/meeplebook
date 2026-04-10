package app.meeplebook.core.plays.domain

/**
 * Represents the identity of a player in a play, including their name
 * and optional BGG username and user ID.
 */
data class PlayerIdentity(
    val name: String,
    val username: String?,
    val userId: Long?
) {
    /**
     * Returns true when [other] refers to the same person: names match case-insensitively
     * and both usernames are either both absent or equal case-insensitively.
     * [userId] is intentionally excluded — it may differ between a freshly-added player
     * (null) and a DB-sourced suggestion (0L when no BGG id is stored).
     */
    fun matches(other: PlayerIdentity): Boolean =
        name.equals(other.name, ignoreCase = true) &&
        username == other.username
}