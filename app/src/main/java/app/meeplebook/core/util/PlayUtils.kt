package app.meeplebook.core.util

import app.meeplebook.R
import app.meeplebook.core.ui.StringProvider

/**
 * Formats a list of player names into a comma-separated string.
 */
fun formatPlayerNames(stringProvider: StringProvider, names: List<String>): String {
    return when {
        names.isEmpty() -> stringProvider.get(R.string.players_none)
        names.size <= 3 -> names.joinToString(", ")
        else -> "${names.take(3).joinToString(", ")}, ${stringProvider.get(R.string.players_more, names.size - 3)}"
    }
}