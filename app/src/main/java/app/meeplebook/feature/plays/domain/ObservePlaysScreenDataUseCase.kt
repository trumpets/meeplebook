package app.meeplebook.feature.plays.domain

import app.meeplebook.core.plays.domain.ObservePlayStatsUseCase
import app.meeplebook.core.plays.domain.ObservePlaysUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case that exposes the user's play history and statistics as a stream
 * suitable for presentation on the Plays screen.
 *
 * Combines data from [ObservePlaysUseCase] and [ObservePlayStatsUseCase],
 * transforming the raw play data using [BuildPlaysSectionsUseCase] to organize
 * plays into chronological sections by month/year.
 *
 * The returned [Flow] will emit updated [DomainPlaysScreenData] whenever the
 * underlying play data or statistics change.
 *
 * @property observePlays Use case that provides the raw play history data.
 * @property sectionBuilder Use case that organizes plays into chronological sections.
 * @property observePlayStats Use case that provides aggregated play statistics.
 */
class ObservePlaysScreenDataUseCase @Inject constructor(
    private val observePlays: ObservePlaysUseCase,
    private val sectionBuilder: BuildPlaysSectionsUseCase,
    private val observePlayStats: ObservePlayStatsUseCase
) {

    /**
     * Observes the plays screen data, combining play history and statistics.
     *
     * @param query Search query to filter plays by game name.
     *
     * @return Flow emitting [DomainPlaysScreenData] containing organized play sections
     * and aggregated statistics.
     */
    operator fun invoke(
        query: String? = null
    ): Flow<DomainPlaysScreenData> {

        return combine(
            observePlays(query),
            observePlayStats()
        ) { plays, playStats ->

            DomainPlaysScreenData(
                sections = sectionBuilder(plays),
                stats = playStats
            )
        }
    }
}