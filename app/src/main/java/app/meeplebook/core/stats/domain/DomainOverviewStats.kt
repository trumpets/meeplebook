package app.meeplebook.core.stats.domain

import app.meeplebook.core.stats.model.CollectionPlayStats

data class DomainOverviewStats(
    val gamesCount: Long,
    val totalPlays: Long,
    val playsInPeriod: Long,
    val unplayedCount: Long
)

/**
 * Maps a [CollectionPlayStats] to a [DomainOverviewStats] for overview display.
 */
fun CollectionPlayStats.toDomain(): DomainOverviewStats {
    return DomainOverviewStats(
        gamesCount = gamesCount,
        totalPlays = totalPlays,
        playsInPeriod = playsInPeriod,
        unplayedCount = unplayedCount
    )
}