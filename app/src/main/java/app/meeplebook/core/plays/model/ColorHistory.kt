package app.meeplebook.core.plays.model

/**
 * Represents colors previously used for a specific game.
 * Used to show frequently used colors when adding players to a play.
 *
 * @param color The color name.
 * @param useCount The number of times this color has been used for this game.
 */
data class ColorHistory(
    val color: String,
    val useCount: Int
)
