package app.meeplebook.feature.overview

import app.meeplebook.R
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.stats.model.CollectionPlayStats
import app.meeplebook.core.ui.StringProvider
import app.meeplebook.core.util.formatPlayerNames
import app.meeplebook.core.util.formatRelativeDate
import app.meeplebook.core.collection.domain.DomainGameHighlight
import app.meeplebook.core.stats.domain.DomainOverviewStats
import app.meeplebook.core.plays.domain.DomainRecentPlay
import app.meeplebook.core.collection.domain.HighlightType

/**
 * Maps a [DomainGameHighlight] to a [GameHighlight] for overview display.
 */
fun DomainGameHighlight.toGameHighlight(stringProvider: StringProvider): GameHighlight {
    return GameHighlight(
        id = id,
        gameName = gameName,
        thumbnailUrl = thumbnailUrl,
        subtitleText = stringProvider.get(
            when (highlightType) {
                HighlightType.RECENTLY_ADDED -> R.string.game_highlight_recently_added
                HighlightType.SUGGESTED -> R.string.game_highlight_try_tonight
            }
        )
    )
}

/**
 * Maps a [DomainRecentPlay] to a [RecentPlay] for overview display.
 */
fun DomainRecentPlay.toRecentPlay(stringProvider: StringProvider): RecentPlay {
    return RecentPlay(
        id = id,
        gameName = gameName,
        thumbnailUrl = thumbnailUrl,
        dateText = formatRelativeDate(stringProvider, date),
        playerCount = playerCount,
        playerNames = formatPlayerNames(stringProvider, playerNames)
    )
}

/**
 * Maps a [CollectionPlayStats] to a [OverviewStats] for overview display.
 */
fun DomainOverviewStats.toOverviewStats(): OverviewStats {
    return OverviewStats(
        gamesCount = gamesCount,
        totalPlays = totalPlays,
        playsThisMonth = playsInPeriod,
        unplayedCount = unplayedCount
    )
}