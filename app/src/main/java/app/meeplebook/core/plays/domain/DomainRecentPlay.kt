package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.model.Play
import java.time.Instant

data class DomainRecentPlay(
    val id: Long,
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
        id = id,
        gameName = gameName,
        thumbnailUrl = null, // TODO: Add thumbnail support from CollectionRepository by mapping Play.gameId to collection items
        date = date,
        playerCount = players.size,
        playerNames = players.map { it.name }
    )
}