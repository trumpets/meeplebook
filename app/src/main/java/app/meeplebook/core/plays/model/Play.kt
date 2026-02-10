package app.meeplebook.core.plays.model

import java.time.Instant

/**
 * Represents a logged play of a game on BGG.
 *
 * @property playId The unique play ID, either local or from BGG.
 * @property date The date and time when the play occurred, as a [java.time.Instant] (UTC).
 * @property quantity The number of times the game was played.
 * @property length The length of the play in minutes.
 * @property incomplete Whether the play was incomplete.
 * @property location The location where the play occurred.
 * @property gameId The BGG game ID.
 * @property gameName The name of the game.
 * @property comments Comments about the play.
 * @property players List of players who participated in the play.
 */
data class Play(
    val playId: PlayId,
    val date: Instant,
    val quantity: Int,
    val length: Int?,
    val incomplete: Boolean,
    val location: String?,
    val gameId: Long,
    val gameName: String,
    val comments: String?,
    val players: List<Player>,
    val syncStatus: PlaySyncStatus
)