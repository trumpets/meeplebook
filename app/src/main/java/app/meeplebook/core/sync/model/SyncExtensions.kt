package app.meeplebook.core.sync.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Watches a user-initiated refresh against an existing sync-state flow and invokes
 * [onRefreshComplete] once the flow transitions from syncing back to idle.
 *
 * The helper intentionally ignores leading `false` emissions so background/app-start syncs do not
 * auto-complete a refresh indicator that the user never started.
 */
fun Flow<SyncState>.observeRefreshCompletion(
    scope: CoroutineScope,
    onRefreshComplete: () -> Unit
): Job {
    return map { it.isSyncing }
        .distinctUntilChanged()
        .dropWhile { !it } // ignore leading false before sync starts
        .onEach { isSyncing ->
            if (!isSyncing) onRefreshComplete()
        }
        .launchIn(scope)
}
