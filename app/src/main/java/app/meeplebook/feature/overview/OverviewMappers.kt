package app.meeplebook.feature.overview

import androidx.annotation.StringRes
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.stats.CollectionPlayStats

/**
 * Maps a [CollectionItem] to a [GameHighlight] for overview display.
 */
fun CollectionItem.toGameHighlight(@StringRes subtitleResId: Int): GameHighlight {
    return GameHighlight(
        id = gameId,
        gameName = name,
        thumbnailUrl = thumbnail,
        subtitleResId = subtitleResId
    )

}

/**
 * Maps a [Play] to a [RecentPlay] for overview display.
 */
fun Play.toRecentPlay(): RecentPlay {
    return RecentPlay(
        id = id,
        gameName = gameName,
        thumbnailUrl = null, // TODO: Add thumbnail support from CollectionRepository by mapping Play.gameId to collection items
        date = date,
        playerCount = players.size,
        playerNames = players.map { it.name }
    )
}

fun CollectionPlayStats.toOverviewStats(): OverviewStats {
    return OverviewStats(
        gamesCount = gamesCount,
        totalPlays = totalPlays,
        playsThisMonth = playsInPeriod,
        unplayedCount = unplayedCount
    )
}