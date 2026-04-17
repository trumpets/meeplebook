package app.meeplebook.core.plays.model

/**
 * Colour assigned to a player within a recorded play.
 *
 * @property colorString The display/API string for the colour (e.g. `"Red"`), used when
 * persisting to the database and when communicating with the BGG API.
 * @property hexColor    RGB hex code (`#RRGGBB`) for displaying a colour swatch in the UI.
 */
enum class PlayerColor(
    val colorString: String,
    val hexColor: String,
) {
    RED("Red", "#F44336"),
    YELLOW("Yellow", "#FDD835"),
    BLUE("Blue", "#2196F3"),
    GREEN("Green", "#4CAF50"),
    PURPLE("Purple", "#9C27B0"),
    ORANGE("Orange", "#FF9800"),
    WHITE("White", "#FAFAFA"),
    BLACK("Black", "#212121"),
    NATURAL("Natural", "#D4C5A9"),
    BROWN("Brown", "#795548"),
    TAN("Tan", "#D2B48C"),
    GRAY("Gray", "#9E9E9E"),
    GOLD("Gold", "#C5A028"),
    SILVER("Silver", "#A8B8C4"),
    BRONZE("Bronze", "#7A4B28"),
    IVORY("Ivory", "#F4EDD6"),
    ROSE("Rose", "#E8B4B8"),
    PINK("Pink", "#E91E63"),
    TEAL("Teal", "#009688"),
    ;

    companion object {
        /** Returns the enum value whose [colorString] matches [value] (case-insensitive), or null. */
        fun fromString(value: String?): PlayerColor? =
            value?.let { v -> entries.find { it.colorString.equals(v, ignoreCase = true) } }
    }
}