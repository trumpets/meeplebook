package app.meeplebook.core.util

import app.meeplebook.R
import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.uiText
import app.meeplebook.core.ui.uiTextCombine
import app.meeplebook.core.ui.uiTextRes

/**
 * Formats a list of player names into a comma-separated string.
 */
fun formatPlayerNames(names: List<String>): UiText {
    return when {
        names.isEmpty() -> uiTextRes(R.string.players_none)
        names.size <= 3 -> uiText(names.joinToString(", "))
        else -> uiTextCombine(
            uiText(names.take(3).joinToString(", ")),
            uiText(", "),
            uiTextRes(R.string.players_more, names.size - 3)
        )
    }
}