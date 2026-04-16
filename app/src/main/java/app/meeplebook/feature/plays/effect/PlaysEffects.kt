package app.meeplebook.feature.plays.effect

/**
 * Container for all effects produced by handling a single
 * [app.meeplebook.feature.plays.PlaysEvent].
 *
 * Separating domain and UI effects keeps the pipeline explicit:
 * - [effects] are handled in the ViewModel
 * - [uiEffects] are emitted to the UI layer
 */
data class PlaysEffects(
    val effects: List<PlaysEffect>,
    val uiEffects: List<PlaysUiEffect>
) {
    companion object {
        /** Convenience value for transitions that produce no side effects. */
        val None = PlaysEffects(
            effects = emptyList(),
            uiEffects = emptyList()
        )
    }
}
