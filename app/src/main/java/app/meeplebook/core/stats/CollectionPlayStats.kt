package app.meeplebook.core.stats

data class CollectionPlayStats(
    val gamesCount: Long,
    val totalPlays: Long,
    val playsInPeriod: Long,
    val unplayedCount: Long
)