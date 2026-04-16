package app.meeplebook.feature.collection.effect

/**
 * Domain-side effects produced by Collection state transitions.
 *
 * These effects are handled by [app.meeplebook.feature.collection.CollectionViewModel] rather than
 * directly in the reducer.
 */
sealed interface CollectionEffect {
    /** Perform a collection sync/refresh operation. */
    data object Refresh : CollectionEffect
}
