package app.meeplebook.feature.collection.reducer

import app.meeplebook.core.ui.architecture.Reducer
import app.meeplebook.feature.collection.CollectionBaseState
import app.meeplebook.feature.collection.CollectionEvent
import javax.inject.Inject

/**
 * Root reducer for the Collection feature.
 *
 * It composes the feature-specific sub-reducers in a fixed order so every event is applied against
 * a single reducer-owned [CollectionBaseState].
 */
class CollectionReducer @Inject constructor(
    private val searchReducer: CollectionSearchReducer,
    private val filterReducer: CollectionFilterReducer,
    private val displayReducer: CollectionDisplayReducer
) : Reducer<CollectionBaseState, CollectionEvent> {

    override fun reduce(
        state: CollectionBaseState,
        event: CollectionEvent
    ): CollectionBaseState {

        val afterSearch = searchReducer.reduce(state, event)
        val afterFilter = filterReducer.reduce(afterSearch, event)
        val afterDisplay = displayReducer.reduce(afterFilter, event)

        return afterDisplay
    }
}
