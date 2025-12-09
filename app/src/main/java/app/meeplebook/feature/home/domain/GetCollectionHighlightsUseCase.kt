package app.meeplebook.feature.home.domain

import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.feature.home.GameHighlight
import javax.inject.Inject

/**
 * Use case that determines game highlights for the home screen.
 * Returns a recently added game and a suggested game to play.
 */
class GetCollectionHighlightsUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val playsRepository: PlaysRepository
) {
    /**
     * Gets game highlights for the home screen.
     *
     * @return Pair of (recentlyAdded, suggested) where either can be null
     */
    suspend operator fun invoke(): Pair<GameHighlight?, GameHighlight?> {
        val collection = collectionRepository.getCollection()
        val plays = playsRepository.getPlays()
        
        if (collection.isEmpty()) {
            return null to null
        }
        
        // Recently added: last game in collection (assuming collection is ordered by add date)
        // In a real implementation, we'd have an actual "dateAdded" field
        val recentlyAdded = collection.lastOrNull()?.let { game ->
            GameHighlight(
                id = game.gameId.toLong(),
                gameName = game.name,
                thumbnailUrl = game.thumbnail,
                subtitle = "Recently Added"
            )
        }
        
        // Suggested: an unplayed game from the collection
        val playedGameIds = plays.map { it.gameId }.toSet()
        val unplayedGames = collection.filter { it.gameId !in playedGameIds }
        val suggested = unplayedGames.firstOrNull()?.let { game ->
            GameHighlight(
                id = game.gameId.toLong(),
                gameName = game.name,
                thumbnailUrl = game.thumbnail,
                subtitle = "Try Tonight?"
            )
        }
        
        return recentlyAdded to suggested
    }
}
