package app.meeplebook.feature.overview.domain

import app.meeplebook.R
import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.feature.overview.GameHighlight
import javax.inject.Inject

/**
 * Use case that determines game highlights for the overview screen.
 * Returns a recently added game and a suggested game to play.
 * Uses optimized Room queries for better performance.
 */
class GetCollectionHighlightsUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository
) {
    /**
     * Gets game highlights for the overview screen using optimized database queries.
     *
     * @return Pair of (recentlyAdded, suggested) where either can be null
     */
    suspend operator fun invoke(): Pair<GameHighlight?, GameHighlight?> {

        // Recently added: game with most recent lastModified date
        val recentlyAdded = collectionRepository.getMostRecentlyAddedItem()?.let { game ->
            GameHighlight(
                id = game.gameId.toLong(),
                gameName = game.name,
                thumbnailUrl = game.thumbnail,
                subtitleResId = R.string.game_highlight_recently_added
            )
        }

        // Suggested: first unplayed game from the collection
        val suggested = collectionRepository.getFirstUnplayedGame()?.let { game ->
            GameHighlight(
                id = game.gameId.toLong(),
                gameName = game.name,
                thumbnailUrl = game.thumbnail,
                subtitleResId = R.string.game_highlight_try_tonight
            )
        }

        return recentlyAdded to suggested
    }
}