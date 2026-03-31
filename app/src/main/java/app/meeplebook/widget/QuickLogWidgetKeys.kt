package app.meeplebook.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.action.ActionParameters

/**
 * DataStore preference keys and Glance action parameter keys used by [QuickLogWidget].
 *
 * State is intentionally flat so it can be written to PreferencesGlanceStateDefinition without
 * a custom serialiser. When the timer feature is implemented, set [hasActivePlay] = true and
 * populate the other keys via [QuickLogWidget.updateActivePlay].
 */
internal object QuickLogWidgetKeys {
    /** Whether there is a play currently in progress. */
    val hasActivePlay = booleanPreferencesKey("qlw_has_active_play")

    /** Local play ID of the active play (only meaningful when [hasActivePlay] is true). */
    val playId = longPreferencesKey("qlw_play_id")

    /** Display name of the game being played (only meaningful when [hasActivePlay] is true). */
    val gameName = stringPreferencesKey("qlw_game_name")

    /**
     * Total elapsed time in seconds accumulated before any current running period.
     * Only meaningful when [hasActivePlay] is true.
     */
    val elapsedSeconds = longPreferencesKey("qlw_elapsed_seconds")

    /**
     * Whether the timer is currently counting up.
     * Only meaningful when [hasActivePlay] is true.
     */
    val isTimerRunning = booleanPreferencesKey("qlw_is_timer_running")

    /** Action parameter key carrying the play ID from a widget tap to an [ActionCallback]. */
    val playIdActionParam = ActionParameters.Key<Long>("qlw_action_play_id")
}
