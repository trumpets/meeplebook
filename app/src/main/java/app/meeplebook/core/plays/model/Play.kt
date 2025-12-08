package app.meeplebook.core.plays.model

/**
 * Represents a logged play of a game on BGG.
 *
 * @param id The unique play ID from BGG.
 * @param date The date when the play occurred (ISO 8601 format: YYYY-MM-DD).
 * @param quantity The number of times the game was played.
 * @param length The length of the play in minutes.
 * @param incomplete Whether the play was incomplete.
 * @param location The location where the play occurred.
 * @param gameId The BGG game ID.
 * @param gameName The name of the game.
 * @param gameSubtype The type of the game (boardgame or boardgameexpansion).
 * @param comments Comments about the play.
 * @param players List of players who participated in the play.
 */
data class Play(
    val id: Int,
    val date: String,
    val quantity: Int,
    val length: Int?,
    val incomplete: Boolean,
    val location: String?,
    val gameId: Int,
    val gameName: String,
    val gameSubtype: String,
    val comments: String?,
    val players: List<Player>
)

/**
 * Represents a player in a play.
 *
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
    val playId: Int,
    val username: String?,
    val userId: Int?,
    val name: String,
    val startPosition: String?,
    val color: String?,
    val score: String?,
    val win: Boolean
)
