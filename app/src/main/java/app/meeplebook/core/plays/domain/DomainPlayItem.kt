package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.plays.model.PlaySyncStatus
import java.time.Instant

/**
 * Domain representation of a play item for the plays feature.
 *
 * This is an intermediate model between the data layer [Play] and the UI layer.
 * It contains enriched data such as player details and is optimized for display logic.
 *
 * @property playId Unique identifier for the play record.
 * @property gameName Name of the game that was played.
 * @property thumbnailUrl Optional URL to the game's thumbnail image.
 * @property date The instant when the play occurred.
 * @property durationMinutes Duration of the play in minutes, if recorded.
 * @property players List of players who participated in the play.
 * @property location Optional location where the game was played.
 * @property comments Optional user comments about the play.
 * @property syncStatus Current synchronization status with BGG.
 */
data class DomainPlayItem(
    val playId: PlayId,
    val gameName: String,
    val thumbnailUrl: String?,
    val date: Instant,
    val durationMinutes: Int?,
    val players: List<DomainPlayerItem>,
    val location: String?,
    val comments: String?,
    val syncStatus: PlaySyncStatus
)

/**
 * Maps a [Play] to a [DomainPlayItem] for plays display.
 */
fun Play.toDomainPlayItem(): DomainPlayItem {
    return DomainPlayItem(
        playId = playId,
        gameName = gameName,
        thumbnailUrl = null, // TODO: Add thumbnail support from CollectionRepository by mapping Play.gameId to collection items
        date = date,
        durationMinutes = length,
        players = players.map { it.toDomainPlayerItem() },
        location = location,
        comments = comments,
        syncStatus = syncStatus
    )
}