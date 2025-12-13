package app.meeplebook.core.util

import android.content.Context
import app.meeplebook.R

/**
 * Formats a list of player names into a comma-separated string.
 */
fun formatPlayerNames(context: Context, names: List<String>): String {
    return when {
        names.isEmpty() -> context.getString(R.string.players_none)
        names.size <= 3 -> names.joinToString(", ")
        else -> "${names.take(3).joinToString(", ")}, ${context.getString(R.string.players_more, names.size - 3)}"
    }
}