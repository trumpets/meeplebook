package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayId
import java.time.Instant

/**
 * Lightweight domain model representing a recent play.
 *
 * This model is intentionally denormalized for fast display: it contains the
 * play identifier, the game's display name and optional thumbnail, the play
 * timestamp, and a small summary of players (count + names).
 *
 * @property playId Identifier of the play (local or remote).
 * @property gameName Display name of the game at the time of the play.
 * @property thumbnailUrl Optional URL to a thumbnail image for the game (nullable when not available).
 * @property date Instant when the play occurred.
 * @property playerCount Number of players involved in the play.
 * @property playerNames Ordered list of player display names.
 */
data class DomainRecentPlay(
    val playId: PlayId,
    val gameName: String,
    val thumbnailUrl: String?,
    val date: Instant,
    val playerCount: Int,
    val playerNames: List<String>
)

/**
 * Maps a [Play] to a [DomainRecentPlay].
 */
fun Play.toDomain(): DomainRecentPlay {
    return DomainRecentPlay(
        playId = playId,
        gameName = gameName,
        thumbnailUrl = null, // TODO: Add thumbnail support from CollectionRepository by mapping Play.gameId to collection items
        date = date,
        playerCount = players.size,
        playerNames = players.map { it.name }
    )
}