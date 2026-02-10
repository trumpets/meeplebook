package app.meeplebook.core.plays.remote.dto

/**
 * Data Transfer Object representing a player as returned by the remote service (BGG).
 *
 * This DTO mirrors the remote API's player representation and is used during
 * network parsing and synchronization. It is mapped to local player entities
 * when saving remote plays into the local database.
 *
 * @property username Optional BGG username associated with the player.
 * @property userId Optional numeric user id from the remote service.
 * @property name Display name of the player (non-null).
 * @property startPosition Optional starting position or seat for this player (string as provided by remote API).
 * @property color Optional color/team chosen by the player, if reported.
 * @property score Optional integer score recorded for the player in the play.
 * @property win True if the player was recorded as the winner of the play.
 */
data class RemotePlayerDto(
    val username: String?,
    val userId: Long?,
    val name: String,
    val startPosition: String?,
    val color: String?,
    val score: Int?,
    val win: Boolean
)