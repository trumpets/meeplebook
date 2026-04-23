package app.meeplebook.feature.overview

import app.meeplebook.R
import app.meeplebook.core.collection.domain.DomainGameHighlight
import app.meeplebook.core.collection.domain.HighlightType
import app.meeplebook.core.plays.domain.DomainRecentPlay
import app.meeplebook.core.stats.domain.DomainOverviewStats
import app.meeplebook.core.sync.model.SyncState
import app.meeplebook.core.sync.toFullSyncStatusUiText
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.core.util.formatPlayerNames
import app.meeplebook.core.util.formatRelativeDate
import app.meeplebook.feature.overview.domain.DomainOverview

/**
 * Maps a domain collection highlight to the UI model consumed by Overview highlight cards.
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
 * Maps a domain recent play snapshot to the UI model consumed by Overview recent-play cards.
 */
fun DomainRecentPlay.toRecentPlay(): RecentPlay {
    return RecentPlay(
        playId = playId,
        gameName = gameName,
        thumbnailUrl = thumbnailUrl,
        dateUiText = formatRelativeDate(date),
        playerCount = playerCount,
        playerNamesUiText = formatPlayerNames(playerNames)
    )
}

/**
 * Maps domain overview stats to the stats-card model rendered by Overview.
 */
fun DomainOverviewStats.toOverviewStats(): OverviewStats {
    return OverviewStats(
        gamesCount = gamesCount,
        totalPlays = totalPlays,
        playsThisMonth = playsInPeriod,
        unplayedCount = unplayedCount
    )
}

/**
 * Builds the renderable [OverviewUiState.Content] from the current domain overview snapshot plus
 * reducer-owned refresh state.
 */
fun DomainOverview.toContentState(baseState: OverviewBaseState, syncState: SyncState): OverviewUiState.Content {
    return OverviewUiState.Content(
        stats = stats.toOverviewStats(),
        recentPlays = recentPlays.map { it.toRecentPlay() },
        recentlyAddedGame = recentlyAddedGame?.toGameHighlight(),
        suggestedGame = suggestedGame?.toGameHighlight(),
        syncStatusUiText = syncState.toFullSyncStatusUiText(),
        isRefreshing = baseState.isRefreshing
    )
}
