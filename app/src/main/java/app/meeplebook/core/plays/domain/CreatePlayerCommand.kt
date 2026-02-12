package app.meeplebook.core.plays.domain

/**
 * Command describing the data required to create a player within a play.
 *
 * This DTO is used when constructing a new play record and captures per-player
 * information such as identity, score and whether the player won.
 *
 * @property username Optional BGG username associated with the player.
 * @property userId Optional numeric user id from BGG or local mapping.
 * @property name Display name of the player (required).
 * @property startPosition Optional starting position or seat (e.g. "N", "1").
 * @property color Optional color/team chosen by the player.
 * @property score Optional score achieved by the player in this play.
 * @property win True if the player won the play.
 */
data class CreatePlayerCommand(
    val username: String?,
    val userId: Long?,
    val name: String,
    val startPosition: String?,
    val color: String?,
    val score: Int?,
    val win: Boolean
)