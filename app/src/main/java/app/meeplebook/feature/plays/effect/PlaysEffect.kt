package app.meeplebook.feature.plays.effect

/**
 * Domain-side effects produced by Plays state transitions.
 *
 * These are handled by the ViewModel rather than the UI. They typically trigger repository or
 * use-case work that should not run inside the reducer itself.
 */
sealed interface PlaysEffect {
    /** Perform screen-open sync orchestration for Plays. */
    data object ScreenOpened : PlaysEffect

    /** Perform a plays sync/refresh operation. */
    data object Refresh : PlaysEffect
}
