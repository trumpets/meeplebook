package app.meeplebook.core.plays.model

/**
 * Colour assigned to a player within a recorded play.
 *
 * @property colorString The display/API string for the colour (e.g. `"Red"`), used when
 * persisting to the database and when communicating with the BGG API.
 */
enum class PlayerColor(
    val colorString: String
) {
    RED("Red"),
    BLUE("Blue"),
    GREEN("Green"),
    YELLOW("Yellow"),
    ORANGE("Orange"),
}