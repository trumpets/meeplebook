package app.meeplebook.feature.collection.reducer

import app.meeplebook.core.ui.architecture.Reducer
import app.meeplebook.feature.collection.CollectionBaseState
import app.meeplebook.feature.collection.CollectionEvent
import javax.inject.Inject

/**
 * Reducer for Collection quick-filter selection.
 */
class CollectionFilterReducer @Inject constructor() : Reducer<CollectionBaseState, CollectionEvent> {

    override fun reduce(
        state: CollectionBaseState,
        event: CollectionEvent
    ): CollectionBaseState {
        return when (event) {
            is CollectionEvent.FilterEvent.QuickFilterSelected -> {
                state.copy(quickFilter = event.filter)
            }

            else -> state
        }
    }
}
