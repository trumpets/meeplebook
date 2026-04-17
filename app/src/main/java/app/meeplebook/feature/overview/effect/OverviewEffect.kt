package app.meeplebook.feature.overview.effect

/**
 * Domain effects produced by Overview events.
 *
 * These effects represent work the [app.meeplebook.feature.overview.OverviewViewModel] must perform
 * outside the synchronous reducer step.
 */
sealed interface OverviewEffect {
    /**
     * Synchronizes user data and updates reducer-owned refresh/error state from the result.
     */
    data object Refresh : OverviewEffect
}
