package app.meeplebook.feature.collection.reducer

import app.meeplebook.core.ui.architecture.Reducer
import app.meeplebook.feature.collection.CollectionBaseState
import app.meeplebook.feature.collection.CollectionEvent
import javax.inject.Inject

/**
 * Reducer for Collection search-query changes.
 */
class CollectionSearchReducer @Inject constructor(): Reducer<CollectionBaseState, CollectionEvent> {

    override fun reduce(
        state: CollectionBaseState,
        event: CollectionEvent
    ): CollectionBaseState {
        return when (event) {
            is CollectionEvent.SearchEvent.SearchChanged -> {
                state.copy(searchQuery = event.query)
            }

            else -> state
        }
    }
}
