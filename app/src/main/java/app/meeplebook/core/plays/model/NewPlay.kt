package app.meeplebook.core.plays.model

import java.time.Instant

/**
 * Represents a new play being created/edited before saving to BGG.
 *
 * @param date The date and time when the play occurred.
 * @param gameId The BGG game ID.
 * @param gameName The name of the game.
 * @param length The length of the play in minutes.
 * @param location The location where the play occurred.
 * @param players List of players who participated in the play.
 * @param comments Comments about the play.
 * @param quantity The number of times the game was played (defaults to 1).
 * @param incomplete Whether the play was incomplete (defaults to false).
 */
data class NewPlay(
    val date: Instant,
    val gameId: Long,
    val gameName: String,
    val length: Int? = null,
    val location: String? = null,
    val players: List<NewPlayer> = emptyList(),
    val comments: String? = null,
    val quantity: Int = 1,
    val incomplete: Boolean = false
)
