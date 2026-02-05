package app.meeplebook.core.ui.flow

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Creates a flow that handles search queries with debouncing and distinct until changed.
 *
 * For empty queries, the block is called immediately. For non-empty queries, the query is debounced
 * before calling the block to avoid excessive calls during typing.
 *
 * @param queryFlow the flow of search query strings
 * @param debounceMillis the debounce time in milliseconds for non-empty queries
 * @param block a function that takes a query string and returns a Flow<T> of results
 * @return a Flow<T> that emits the search results
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
fun <T> searchableFlow(
    queryFlow: Flow<String>,
    debounceMillis: Long,
    block: (String) -> Flow<T>
): Flow<T> =
    queryFlow
        .map { it.trim() }
        .distinctUntilChanged()
        .debounce { query ->
            // No debounce for empty queries, debounce non-empty queries
            if (query.isEmpty()) 0L else debounceMillis
        }
        .flatMapLatest(block)