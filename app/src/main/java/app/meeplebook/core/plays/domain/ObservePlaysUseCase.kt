package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.PlaysRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case that observes the user's play history.
 *
 * Provides a reactive stream of plays that can be filtered by a search query.
 * The returned flow automatically updates when the underlying play data changes.
 *
 * @property playsRepository Repository for accessing play data.
 */
class ObservePlaysUseCase @Inject constructor(
    private val playsRepository: PlaysRepository
) {

    /**
     * Observes the user's play history as a continuous flow.
     *
     * @param gameOrLocationQuery Optional search query to filter plays by game name or location.
     * If `null` or blank, returns the full play history.
     * @return Flow emitting a list of [DomainPlayItem] representing the user's plays.
     */
    operator fun invoke(gameOrLocationQuery: String? = null): Flow<List<DomainPlayItem>> {
        return playsRepository.observePlays(gameOrLocationQuery).map { items ->
            items.map { item -> item.toDomainPlayItem() }
        }.distinctUntilChanged()
    }
}