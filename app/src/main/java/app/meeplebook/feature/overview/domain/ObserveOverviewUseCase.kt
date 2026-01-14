package app.meeplebook.feature.overview.domain

import app.meeplebook.core.collection.domain.ObserveCollectionHighlightsUseCase
import app.meeplebook.core.plays.domain.ObserveRecentPlaysUseCase
import app.meeplebook.core.stats.domain.ObserveCollectionPlayStatsUseCase
import app.meeplebook.core.sync.domain.ObserveLastFullSyncUseCase
import app.meeplebook.core.collection.domain.HighlightType
import app.meeplebook.core.collection.domain.toDomainGameHighlight
import app.meeplebook.core.plays.domain.toDomain
import app.meeplebook.core.stats.domain.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveOverviewUseCase @Inject constructor(
    private val observeStats: ObserveCollectionPlayStatsUseCase,
    private val observeRecentPlays: ObserveRecentPlaysUseCase,
    private val observeHighlights: ObserveCollectionHighlightsUseCase,
    private val observeLastSync: ObserveLastFullSyncUseCase
) {

    operator fun invoke(): Flow<DomainOverview> =
        combine(
            observeStats(),
            observeRecentPlays(),
            observeHighlights(),
            observeLastSync()
        ) { stats, recentPlays, highlights, lastSync ->

            DomainOverview(
                stats = stats.toDomain(),
                recentPlays = recentPlays.map { play -> play.toDomain() },
                recentlyAddedGame = highlights.recentlyAdded?.toDomainGameHighlight(HighlightType.RECENTLY_ADDED),
                suggestedGame = highlights.suggested?.toDomainGameHighlight(HighlightType.SUGGESTED),
                lastSyncedDate = lastSync
            )
        }
}