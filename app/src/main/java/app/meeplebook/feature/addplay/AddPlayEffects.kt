package app.meeplebook.feature.addplay

/**
 * Container for all side effects produced by a single state transition on the Add Play screen.
 *
 * Keeping domain and UI effects in separate lists lets callers dispatch each kind to
 * the appropriate handler without an `is`-check on a common supertype.
 *
 * Use [AddPlayEffects.None] for transitions that produce no side effects.
 *
 * @property effects    Domain-level effects handled by the ViewModel (e.g., loading data, saving).
 * @property uiEffects  UI-level effects handled by the Composable (e.g., navigation, snackbars).
 * @see AddPlayEffect
 * @see AddPlayUiEffect
 */
data class AddPlayEffects(
    val effects: List<AddPlayEffect>,
    val uiEffects: List<AddPlayUiEffect>
) {
    companion object {
        /** Convenience value for transitions that produce no side effects. */
        val None = AddPlayEffects(effects = emptyList(), uiEffects = emptyList())
    }
}
