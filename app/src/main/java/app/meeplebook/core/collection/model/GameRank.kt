package app.meeplebook.core.collection.model

import kotlinx.serialization.Serializable

/**
 * Represents a BGG ranking entry for a game.
 *
 * @property type Whether this is a subtype rank (e.g. "Board Game Rank") or a family rank
 *   (e.g. "Strategy Game Rank", "Thematic Rank").
 * @property name The internal BGG rank name (e.g. "boardgame", "strategygames").
 * @property friendlyName The human-readable rank name (e.g. "Board Game Rank", "Strategy Game Rank").
 * @property value The rank position. Null when the game is "Not Ranked".
 */
@Serializable
data class GameRank(
    val type: RankType,
    val name: String,
    val friendlyName: String,
    val value: Int?
)

/**
 * The kind of BGG ranking.
 */
@Serializable
enum class RankType {
    /** The main BGG ranking (type="subtype" in the API). */
    SUBTYPE,

    /** A genre or family ranking such as Strategy, Thematic, etc. (type="family" in the API). */
    FAMILY
}
