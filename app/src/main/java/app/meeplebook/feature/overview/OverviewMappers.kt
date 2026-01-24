package app.meeplebook.feature.overview

import app.meeplebook.R
import app.meeplebook.core.collection.domain.DomainGameHighlight
import app.meeplebook.core.collection.domain.HighlightType
import app.meeplebook.core.plays.domain.DomainRecentPlay
import app.meeplebook.core.stats.domain.DomainOverviewStats
import app.meeplebook.core.stats.model.CollectionPlayStats
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.core.util.formatPlayerNames
import app.meeplebook.core.util.formatRelativeDate

/**
 * Maps a [DomainGameHighlight] to a [GameHighlight] for overview display.
 */
fun DomainGameHighlight.toGameHighlight(): GameHighlight {
    return GameHighlight(
        id = id,
        gameName = gameName,
        thumbnailUrl = thumbnailUrl,
        subtitleUiText = uiTextRes(
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
fun DomainRecentPlay.toRecentPlay(): RecentPlay {
    return RecentPlay(
        id = id,
        gameName = gameName,
        thumbnailUrl = thumbnailUrl,
        dateUiText = formatRelativeDate(date),
        playerCount = playerCount,
        playerNamesUiText = formatPlayerNames(playerNames)
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