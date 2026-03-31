package app.meeplebook.core.plays.model

/**
 * Colour assigned to a player within a recorded play.
 *
 * @property colorString The display/API string for the colour (e.g. `"Red"`), used when
 * persisting to the database and when communicating with the BGG API.
 * @property hexColor    ARGB hex code (`#RRGGBB`) for displaying a colour swatch in the UI.
 */
enum class PlayerColor(
    val colorString: String,
    val hexColor: String,
) {
    RED("Red", "#F44336"),
    BLUE("Blue", "#2196F3"),
    GREEN("Green", "#4CAF50"),
    YELLOW("Yellow", "#FDD835"),
    ORANGE("Orange", "#FF9800"),
    PURPLE("Purple", "#9C27B0"),
    BLACK("Black", "#212121"),
    WHITE("White", "#FAFAFA"),
    TEAL("Teal", "#009688"),
    PINK("Pink", "#E91E63"),
    ;

    companion object {
        /** Returns the enum value whose [colorString] matches [value] (case-insensitive), or null. */
        fun fromString(value: String?): PlayerColor? =
            value?.let { v -> entries.find { it.colorString.equals(v, ignoreCase = true) } }
    }
}