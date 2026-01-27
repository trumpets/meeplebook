package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.model.Player

/**
 * Domain representation of a player in a play record.
 *
 * This is an intermediate model between the data layer [Player] and the UI layer,
 * containing player-specific information for a single play session.
 *
 * @property name The player's name or username.
 * @property startPosition The player's starting position in the game, if applicable.
 * @property score The player's final score, if recorded.
 * @property win Whether the player won this play session.
 */
data class DomainPlayerItem(
    val name: String,
    val startPosition: String?,
    val score: Int?,
    val win: Boolean
)

/**
 * Maps a [Player] to a [DomainPlayerItem] for plays display.
 */
fun Player.toDomainPlayerItem(): DomainPlayerItem {
    return DomainPlayerItem(
        name = name,
        startPosition = startPosition,
        score = score,
        win = win
    )
}