package app.meeplebook.core.plays.model

/**
 * Represents a play logged in BGG.
 */
data class Play(
    val playId: Long,
    val date: String,
    val quantity: Int,
    val length: Int,
    val incomplete: Boolean,
    val noWinStats: Boolean,
    val location: String?,
    val comments: String?,
    val game: PlayGame,
    val players: List<PlayPlayer>
)

/**
 * Represents the game associated with a play.
 */
data class PlayGame(
    val objectId: Long,
    val name: String
)

/**
 * Represents a player in a play.
 */
data class PlayPlayer(
    val username: String?,
    val name: String,
    val startPosition: String?,
    val color: String?,
    val score: String?,
    val new: Boolean,
    val win: Boolean
)
