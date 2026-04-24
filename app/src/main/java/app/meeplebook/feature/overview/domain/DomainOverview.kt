package app.meeplebook.feature.overview.domain

import app.meeplebook.core.collection.domain.DomainGameHighlight
import app.meeplebook.core.plays.domain.DomainRecentPlay
import app.meeplebook.core.stats.domain.DomainOverviewStats

data class DomainOverview(
    val stats: DomainOverviewStats,
    val recentPlays: List<DomainRecentPlay>,
    val recentlyAddedGame: DomainGameHighlight?,
    val suggestedGame: DomainGameHighlight?
)