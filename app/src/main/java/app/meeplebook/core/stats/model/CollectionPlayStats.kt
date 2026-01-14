package app.meeplebook.core.stats.model

data class CollectionPlayStats(
    val gamesCount: Long,
    val totalPlays: Long,
    val playsInPeriod: Long,
    val unplayedCount: Long
)