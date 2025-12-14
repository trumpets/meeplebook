package app.meeplebook.feature.overview.domain

import app.meeplebook.R
import app.meeplebook.core.collection.domain.ObserveCollectionHighlightsUseCase
import app.meeplebook.core.plays.domain.ObserveRecentPlaysUseCase
import app.meeplebook.core.stats.ObserveCollectionPlayStatsUseCase
import app.meeplebook.core.sync.domain.ObserveLastFullSyncUseCase
import app.meeplebook.feature.overview.OverviewUiState
import app.meeplebook.feature.overview.toGameHighlight
import app.meeplebook.feature.overview.toOverviewStats
import app.meeplebook.feature.overview.toRecentPlay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveOverviewUseCase @Inject constructor(
    private val observeStats: ObserveCollectionPlayStatsUseCase,
    private val observeRecentPlays: ObserveRecentPlaysUseCase,
    private val observeHighlights: ObserveCollectionHighlightsUseCase,
    private val observeLastSync: ObserveLastFullSyncUseCase
) {

    operator fun invoke(): Flow<OverviewUiState> =
        combine(
            observeStats(),
            observeRecentPlays(),
            observeHighlights(),
            observeLastSync()
        ) { stats, recentPlays, highlights, lastSync ->

            OverviewUiState(
                stats = stats.toOverviewStats(),
                recentPlays = recentPlays.map { play -> play.toRecentPlay() },
                recentlyAddedGame = highlights.recentlyAdded?.toGameHighlight(R.string.game_highlight_recently_added),
                suggestedGame = highlights.suggested?.toGameHighlight(R.string.game_highlight_try_tonight),
                lastSyncedDate = lastSync,
                isLoading = true
            )
        }
}