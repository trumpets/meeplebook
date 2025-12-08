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
    val comments: String?,
    val players: List<Player>
)